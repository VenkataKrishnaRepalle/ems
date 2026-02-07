package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.config.KeycloakAdminConfig.KeycloakAdminProperties;
import com.learning.emsmybatisliquibase.dto.KeycloakCreateUserDto;
import com.learning.emsmybatisliquibase.service.KeycloakService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class KeycloakServiceImpl implements KeycloakService {

    private final WebClient adminWebClient;
    private final WebClient tokenWebClient;
    private final KeycloakAdminProperties properties;

    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    public KeycloakServiceImpl(WebClient webClient, KeycloakAdminProperties properties) {
        this.properties = properties;

        String baseUrl = ensureNoTrailingSlash(require(properties.baseUrl(), "keycloak.admin.base-url"));
        String realm = require(properties.realm(), "keycloak.admin.realm");

        this.adminWebClient = webClient.mutate()
                .baseUrl(baseUrl + "/admin/realms/" + realm)
                .build();

        this.tokenWebClient = webClient.mutate()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public void create(KeycloakCreateUserDto userDto) {
        String token = getAdminAccessToken();

        adminWebClient
                .post()
                .uri("/users")
                .headers(headers -> headers.setBearerAuth(token))
                .bodyValue(userDto)
                .retrieve()
                .onStatus(
                        status -> status.value() != 201,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("Failed to create user in Keycloak: status={} body={}", response.statusCode().value(), body);
                                    return Mono.error(new IllegalStateException("Failed to create user in Keycloak"));
                                })
                )
                .toBodilessEntity()
                .doOnSuccess(ignored -> log.info("User created successfully in Keycloak"))
                .block();
    }

    private String getAdminAccessToken() {
        CachedToken current = cachedToken.get();
        if (current != null && !current.isExpiredSoon()) {
            return current.accessToken();
        }

        CachedToken refreshed = requestClientCredentialsToken();
        cachedToken.set(refreshed);
        return refreshed.accessToken();
    }

    private CachedToken requestClientCredentialsToken() {
        String realm = require(properties.realm(), "keycloak.admin.realm");
        String clientId = require(properties.clientId(), "keycloak.admin.client-id");
        String clientSecret = require(properties.clientSecret(), "keycloak.admin.client-secret");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        KeycloakTokenResponse tokenResponse = tokenWebClient
                .post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("Failed to fetch Keycloak token: status={} body={}", response.statusCode().value(), body);
                                    return Mono.error(new IllegalStateException("Failed to fetch Keycloak token"));
                                })
                )
                .bodyToMono(KeycloakTokenResponse.class)
                .block(Duration.ofSeconds(10));

        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.access_token())) {
            throw new IllegalStateException("Keycloak token response missing access_token");
        }

        long expiresInSeconds = tokenResponse.expires_in() == null ? 60 : Math.max(1, tokenResponse.expires_in());
        log.info("Fetched Keycloak admin access token (expires_in={}s, client_id={}, token= {})", expiresInSeconds, properties.clientId(), tokenResponse.access_token());
        return CachedToken.fromNow(tokenResponse.access_token(), expiresInSeconds);
    }

    private static String ensureNoTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String require(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing required property: " + name);
        }
        return value;
    }

    private record KeycloakTokenResponse(String access_token, Long expires_in) {}

    private record CachedToken(String accessToken, long expiresAtEpochMillis) {
        static CachedToken fromNow(String accessToken, long expiresInSeconds) {
            long expiresAt = System.currentTimeMillis() + Duration.ofSeconds(expiresInSeconds).toMillis();
            return new CachedToken(accessToken, expiresAt);
        }

        boolean isExpiredSoon() {
            return System.currentTimeMillis() >= (expiresAtEpochMillis - Duration.ofSeconds(30).toMillis());
        }
    }
}
