package com.youda.classroom.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 上传文件下载加固：对 /files/** 静态资源强制「附件下载」语义并禁止 MIME 嗅探。
 * 与上传扩展名白名单（ResourceService）形成纵深防御，防止上传的 HTML/SVG 等文件
 * 被同源内联渲染成存储型 XSS。
 */
@Component
public class FileDownloadHeaderFilter extends OncePerRequestFilter {

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
        String filename = path.substring(path.lastIndexOf('/') + 1);
        // 强制以附件形式下载（不内联渲染）；禁止 MIME 嗅探，防内容被当作可执行脚本
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        filterChain.doFilter(request, response);
    }
}
