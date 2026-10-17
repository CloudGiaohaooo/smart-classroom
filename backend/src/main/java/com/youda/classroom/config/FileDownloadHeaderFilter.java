package com.youda.classroom.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youda.classroom.common.Result;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.security.SignedFileUrl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 上传文件下载加固 + 鉴权（Phase 20）：
 * 1. 对 /files/** 校验签名 URL（expires + sign），匿名 / 过期 / 篡改的直连下载一律拒绝；
 * 2. 对合法请求强制「附件下载」语义 + 禁止 MIME 嗅探，防止上传的 HTML/SVG 等文件被同源内联渲染成存储型 XSS。
 */
@Component
@RequiredArgsConstructor
public class FileDownloadHeaderFilter extends OncePerRequestFilter {

    private final SignedFileUrl signedFileUrl;
    private final ObjectMapper objectMapper;

    @Value("${file.access-prefix}")
    private String accessPrefix;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.equals(accessPrefix) || path.startsWith(accessPrefix + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        // 签名鉴权：仅接受携带有效签名且未过期的下载链接（签名覆盖路径，防跨路径 / 匿名直连）
        if (!signedFileUrl.verify(path, parseLong(request.getParameter("expires")), request.getParameter("sign"))) {
            writeUnauthorized(response);
            return;
        }

        String filename = path.substring(path.lastIndexOf('/') + 1);
        // 强制以附件形式下载（不内联渲染）；禁止 MIME 嗅探，防内容被当作可执行脚本
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        // 禁止缓存下载内容，避免敏感资源被本地/中间层缓存复用
        response.setHeader("Cache-Control", "no-store");
        filterChain.doFilter(request, response);
    }

    private long parseLong(String value) {
        try {
            return value == null ? -1 : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        // 项目约定：HTTP 恒 200，业务码区分（401 = 未授权访问下载资源）
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(new String(
                objectMapper.writeValueAsBytes(Result.fail(ResultCode.UNAUTHORIZED, "文件下载链接无效或已过期")),
                StandardCharsets.UTF_8));
    }
}
