package com.learning.emsmybatisliquibase.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KeycloakAdminConfig.KeycloakAdminProperties.class)
public class KeycloakAdminConfig {

    @ConfigurationProperties(prefix = "keycloak.admin")
    public record KeycloakAdminProperties(
            String baseUrl,
            String realm,
            String clientId,
            String clientSecret
    ) {}
}

