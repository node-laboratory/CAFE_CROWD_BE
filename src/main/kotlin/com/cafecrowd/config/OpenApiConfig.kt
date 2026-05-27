package com.cafecrowd.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun cafeCrowdOpenAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Cafe Crowd API")
                    .description("카페붐빔 백엔드 API")
                    .version("v1")
                    .contact(Contact().name("Cafe Crowd Team"))
                    .license(License().name("Proprietary"))
            )
            .servers(
                listOf(
                    Server().url("http://localhost:8080").description("Local"),
                    Server().url("https://api-stg.cafecrowd.example.com").description("Staging"),
                    Server().url("https://api.cafecrowd.example.com").description("Production"),
                )
            )
            .components(
                Components().addSecuritySchemes(
                    "bearer-auth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("Token")
                )
            )
}
