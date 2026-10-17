package com.youda.classroom.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置：
 * 将本地上传目录映射为静态资源，供前端访问文件。
 *
 * 注意：鉴权与跨域已迁移至 Spring Security（见 SecurityConfig）——
 * 原 JwtInterceptor 与 addCorsMappings 已在 Phase 2 移除，避免与 Security 过滤链重复。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.access-prefix}")
    private String accessPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 将 /files/** 映射到本地上传目录
        registry.addResourceHandler(accessPrefix + "/**")
                .addResourceLocations("file:" + new File(uploadDir).getAbsolutePath() + File.separator);
    }
}
