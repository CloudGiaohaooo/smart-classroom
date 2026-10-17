package com.youda.classroom.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youda.classroom.security.SignedFileUrl;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 文件下载过滤器单测（Phase 20）：验证 /files/** 签名鉴权 + 下载加固响应头。
 */
class FileDownloadHeaderFilterTest {

    private static final String SECRET = "YouDaTestSecretKey-0123456789abcdefghijklmnopqrstuv";

    private SignedFileUrl signedFileUrl;
    private FileDownloadHeaderFilter filter;

    @BeforeEach
    void setUp() {
        signedFileUrl = new SignedFileUrl(SECRET, 1800);
        filter = new FileDownloadHeaderFilter(signedFileUrl, new ObjectMapper());
        ReflectionTestUtils.setField(filter, "accessPrefix", "/files");
    }

    @Test
    void shouldNotFilterNonFilePaths() {
        assertTrue(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/courses")));
    }

    @Test
    void validSignatureSetsHeadersAndContinues() throws Exception {
        MockHttpServletRequest req = signedRequest("/files/1/abc.pdf");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertTrue(res.getHeader("Content-Disposition").contains("attachment"));
        assertEquals("nosniff", res.getHeader("X-Content-Type-Options"));
        assertEquals("no-store", res.getHeader("Cache-Control"));
    }

    @Test
    void missingSignatureReturns401AndStopsChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/files/1/abc.pdf");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(200, res.getStatus()); // 项目约定 HTTP 恒 200
        assertTrue(res.getContentAsString().contains("\"code\":401"));
    }

    @Test
    void tamperedSignatureReturns401() throws Exception {
        MockHttpServletRequest req = signedRequest("/files/1/abc.pdf");
        req.setParameter("sign", "deadbeef");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(200, res.getStatus());
        assertTrue(res.getContentAsString().contains("\"code\":401"));
    }

    private MockHttpServletRequest signedRequest(String path) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", path);
        String url = signedFileUrl.sign(path);
        String query = url.substring(url.indexOf('?') + 1);
        for (String kv : query.split("&")) {
            String[] p = kv.split("=");
            req.addParameter(p[0], p[1]);
        }
        return req;
    }
}
