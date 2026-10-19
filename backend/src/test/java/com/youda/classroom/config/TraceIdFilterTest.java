package com.youda.classroom.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 链路追踪过滤器单测（Phase 21）：验证 traceId 生成 / 透传、响应头回写、MDC 清理。
 */
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void generatesTraceIdWhenHeaderAbsent() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/courses");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        String traceId = res.getHeader("X-Trace-Id");
        assertNotNull(traceId);
        assertEquals(16, traceId.length());
        assertNull(MDC.get("traceId")); // 请求结束后已清理，防线程池复用串号
    }

    @Test
    void passesThroughIncomingTraceId() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/courses");
        req.addHeader("X-Trace-Id", "abc123def4567890");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertEquals("abc123def4567890", res.getHeader("X-Trace-Id"));
        assertNull(MDC.get("traceId"));
    }
}
