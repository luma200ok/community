package com.community.community.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // 1. 토큰을 입력받을 수 있는 '보안 스키마' 설정
        SecurityScheme securityScheme = new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // 2. 모든 API 요청에 이 보안 스키마를 적용하겠다고 선언
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement)
                .info(new Info()
                        .title("커뮤니티 프로젝트 API 문서")
                        .description("게시글, 댓글, 회원 인증 기능이 포함된 REST API 명세서 입니다.")
                        .version("v1.0.0"));
    }
}
