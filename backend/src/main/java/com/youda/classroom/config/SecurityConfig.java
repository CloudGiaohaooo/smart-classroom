package com.youda.classroom.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youda.classroom.common.Result;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring Security 配置（Phase 2）：
 * 用完整过滤链替换原「拦截器 + WebConfig」鉴权方案，保留 JWT 作为认证载体。
 *
 * 与拦截器的核心差异：
 * - 拦截器：DispatcherServlet 之后执行，只能做「是否登录」的粗粒度判断，难以做方法级授权；
 * - Security 过滤链：在进入 MVC 之前就完成认证，并配合 @EnableMethodSecurity 做 @PreAuthorize
 *   方法级权限控制（如「只有 TEACHER 才能 submission:grade」），权限模型为 RBAC 四表。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 无状态 JWT，关闭 CSRF；Session 设为 STATELESS
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 放行：登录接口、静态文件、接口文档
                        .requestMatchers("/api/auth/login", "/files/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 其余 /api/** 一律要求登录；细粒度权限交给 @PreAuthorize
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        // 未登录 / token 无效：返回业务码 401（HTTP 仍 200，兼容前端统一拦截）
                        .authenticationEntryPoint((req, res, e) ->
                                writeJson(res, ResultCode.UNAUTHORIZED, "未登录或登录已过期"))
                        // 已登录但无权限（URL 级）：返回业务码 403
                        .accessDeniedHandler((req, res, e) ->
                                writeJson(res, ResultCode.FORBIDDEN, "无权限执行该操作")))
                // 在用户名密码认证过滤器之前插入 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CORS 配置源：与 Security 过滤链整合（原 WebConfig 中的 addCorsMappings 无法作用于过滤链）
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeJson(HttpServletResponse response, ResultCode code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.fail(code, message);
        response.getWriter().write(new String(objectMapper.writeValueAsBytes(result), StandardCharsets.UTF_8));
    }
}
