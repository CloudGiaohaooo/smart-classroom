package com.youda.classroom.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器（可观测性，Phase 21）：
 * 为每个请求生成 / 透传一个 traceId，写入日志 MDC 与响应头 {@code X-Trace-Id}，
 * 使同一请求在「多行日志 / 前端报错 / 网关日志」之间可被唯一关联，便于排障。
 *
 * 注册方式：{@code @Component} 自动注册为 Servlet 过滤器，{@code @Order(HIGHEST_PRECEDENCE)}
 * 保证其在 Spring Security 过滤链（order = -100）之前执行，从而覆盖登录、
 * {@code /files/**} 下载、{@code @PreAuthorize} 拒绝等所有路径，包括 401/403 响应。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 透传上游 traceId（网关 / 负载均衡传入时保持全链路一致），否则本地生成一个短 id
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 线程池复用：请求结束必须清理 MDC，防止 traceId 串到下一个请求（同 UserContext.clear 的考虑）
            MDC.remove(MDC_KEY);
        }
    }
}
