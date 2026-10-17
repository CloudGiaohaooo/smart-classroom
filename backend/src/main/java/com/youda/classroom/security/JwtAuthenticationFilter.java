package com.youda.classroom.security;

import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.PermissionMapper;
import com.youda.classroom.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器（Spring Security 过滤链版，替代原 JwtInterceptor）。
 *
 * 与原拦截器的差异：拦截器工作在 DispatcherServlet 之后（MVC 层），只能做粗粒度鉴权；
 * 过滤器工作在 Servlet 过滤链（进入 MVC 之前），能：
 * 1. 在请求进入 Controller 前就完成认证，未认证请求直接由 SecurityConfig 的认证入口拦截；
 * 2. 把「RBAC 权限编码」写入 SecurityContext，配合 @PreAuthorize 做方法级细粒度授权；
 * 3. 同时保留 ThreadLocal 上下文，业务代码（如 SubmissionService 读当前用户）零改动。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                try {
                    Claims claims = jwtUtil.parseToken(token);
                    Long userId = claims.get("userId", Long.class);
                    String username = claims.getSubject();
                    String role = claims.get("role", String.class);

                    // 复查账号是否仍存在且未被禁用（status==0 表示禁用）：账号被禁用/删除后，
                    // 已签发的 token 立即失效，不写入认证信息，由 SecurityConfig 认证入口统一返回 401
                    User user = userMapper.selectById(userId);
                    boolean accountActive = user != null && (user.getStatus() == null || user.getStatus() != 0);

                    if (accountActive) {
                        // 从 RBAC 表实时加载权限编码，构建认证对象（权限变更即时生效，无需重发 token）
                        List<SimpleGrantedAuthority> authorities = permissionMapper
                                .selectPermCodesByUserId(userId)
                                .stream().map(SimpleGrantedAuthority::new).toList();
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userId, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);

                        // 保留 ThreadLocal 上下文，业务代码零改动
                        UserContext.set(new UserContext.CurrentUser(userId, username, role));
                    }
                } catch (Exception e) {
                    // token 无效/过期：不写入认证信息，由 SecurityConfig 认证入口统一返回 401
                    SecurityContextHolder.clearContext();
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            // 等价于原 JwtInterceptor.afterCompletion：防止线程池复用导致的 ThreadLocal 泄漏
            UserContext.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
