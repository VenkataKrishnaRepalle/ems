package com.learning.emsmybatisliquibase.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KeycloakAdminConfig.KeycloakAdminProperties.class)
public class KeycloakAdminConfig {

    @ConfigurationProperties(prefix = "keycloak.admin")
    public record KeycloakAdminProperties(
            @NotBlank(message = "Keycloak admin baseurl must not be blank or null or empty")
            String baseUrl,
            @NotBlank(message = "Keycloak realm must not be blank or null or empty")
            String realm
    ) {}
}

