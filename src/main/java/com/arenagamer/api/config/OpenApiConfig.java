package com.arenagamer.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI arenaGamerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ArenaGamer API")
                        .description("""
                                API organizada em três áreas:
                                - **Public** — cadastro/login sem auth; catálogo com HTTP Basic
                                - **Common** — operações autenticadas (staff ou cliente) com JWT Bearer
                                - **Admin** — painel staff com JWT Bearer
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("ArenaGamer").email("support@arenagamer.com")))
                .components(new Components()
                        .addSecuritySchemes("Bearer",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Token JWT obtido em POST /api/v1/public/auth/login"))
                        .addSecuritySchemes("BasicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Email e senha de staff ou cliente — catálogo público")));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("0-all")
                .displayName("Todos")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("1-public")
                .displayName("Public")
                .pathsToMatch("/api/v1/public/**")
                .build();
    }

    @Bean
    public GroupedOpenApi commonApi() {
        return GroupedOpenApi.builder()
                .group("2-common")
                .displayName("Common")
                .pathsToMatch("/api/v1/common/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("3-admin")
                .displayName("Admin")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }
}
