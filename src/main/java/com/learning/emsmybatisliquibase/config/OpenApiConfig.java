package com.learning.emsmybatisliquibase.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "keycloak_auth";
        String normalizedIssuerUri = issuerUri.endsWith("/")
                ? issuerUri.substring(0, issuerUri.length() - 1)
                : issuerUri;
        String authUrl = normalizedIssuerUri + "/protocol/openid-connect/auth";
        String tokenUrl = normalizedIssuerUri + "/protocol/openid-connect/token";
        OAuthFlow authorizationCodeFlow = new OAuthFlow()
                .authorizationUrl(authUrl)
                .tokenUrl(tokenUrl)
                .scopes(new Scopes().addString("openid", "OpenID Connect scope"));

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .flows(new OAuthFlows()
                                                .authorizationCode(authorizationCodeFlow))));
    }
}
