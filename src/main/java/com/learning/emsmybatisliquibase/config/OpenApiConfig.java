package com.learning.emsmybatisliquibase.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Employee Management System API",
                description = "EMS API",
                summary = "API functionalities for EMS",
                termsOfService = "T&C",
                contact = @Contact(
                        name = "Venkata Krishna Repalle",
                        email = "rvkrishna13052001@gmail.com"
                ),
                license = @License(
                        name = "abcd"
                ),
                version = "v3"

        ),
        servers = {
                @Server(
                        description = "Dev",
                        url = "http://localhost:8082"
                ),
                @Server(
                        description = "Test",
                        url = "http://localhost:8082"
                ),
                @Server(
                        description = "Production",
                        url = "https://emssopra.azurewebsites.net"

                )
        }
)
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                );
    }
}