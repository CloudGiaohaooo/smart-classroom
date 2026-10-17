package com.youda.classroom.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置（Phase 5）：
 * 生成接口文档（/v3/api-docs）与 Swagger UI（/swagger-ui/index.html），
 * 并配置 JWT Bearer 认证，便于在 UI 上「Authorize」后直接调试受保护接口。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartClassroomOpenAPI() {
        String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("优达智慧课堂教学管理平台 API")
                        .description("课程 / 作业 / 批阅 / 资源 / 统计接口文档"
                                + "（RBAC + Redis 缓存 + 智能批阅 + 学习预警）")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .name(scheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
