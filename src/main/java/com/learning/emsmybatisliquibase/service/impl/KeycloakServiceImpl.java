package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.config.KeycloakAdminConfig.KeycloakAdminProperties;
import com.learning.emsmybatisliquibase.service.KeycloakService;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;

@Service
@Slf4j
public class KeycloakServiceImpl implements KeycloakService {

    private final RealmResource realmResource;

    public KeycloakServiceImpl(KeycloakAdminProperties properties) {
        String baseUrl = properties.baseUrl();
        String realm = properties.realm();

        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(baseUrl)
                .realm("master")
                .clientId("admin-cli")
                .username("admin")
                .password("admin")
                .build();

        this.realmResource = keycloak.realm(realm);
    }

    @Override
    public String create(UserRepresentation userDto) {
        Response response = realmResource.users().create(userDto);
        String keycloakUserId = extractUserId(response);
        log.info("User created successfully in Keycloak with id={}", keycloakUserId);
        return keycloakUserId;
    }

    @Override
    public String create(UserRepresentation userDto, List<String> roleNames) {
        String userId = create(userDto);
        UserResource userResource = realmResource.users().get(userId);
        if (roleNames != null && !roleNames.isEmpty()) {
            List<RoleRepresentation> roles = roleNames.stream()
                    .map(roleName -> realmResource.roles().get(roleName).toRepresentation())
                    .toList();
            userResource.roles().realmLevel().add(roles);
            log.info("User added roles in Keycloak, id={}", userId);
        }
        return userId;
    }

    @Override
    public void update(UserRepresentation userDto) {
        UserResource userResource = realmResource.users().get(userDto.getId());
        userResource.update(userDto);
        log.info("User updated successfully in Keycloak with id={}", userDto.getId());

    }

    @Override
    public void update(UserRepresentation userDto, List<String> roleNames) {
        UserResource userResource = realmResource.users().get(userDto.getId());
        userResource.update(userDto);
        if (roleNames != null && !roleNames.isEmpty()) {
            List<RoleRepresentation> existingRoles =
                    userResource.roles().realmLevel().listAll();

            if (!existingRoles.isEmpty()) {
                userResource.roles().realmLevel().remove(existingRoles);
            }

            if (!roleNames.isEmpty()) {
                List<RoleRepresentation> newRoles = roleNames.stream()
                        .map(role -> realmResource.roles().get(role).toRepresentation())
                        .toList();

                userResource.roles().realmLevel().add(newRoles);
            }
        }
    }

    private String extractUserId(Response response) {
        if (response == null) {
            throw new IllegalStateException("Keycloak create user response was empty");
        }

        URI location = response.getLocation();
        if (location == null) {
            throw new IllegalStateException("Keycloak create user response missing Location header");
        }

        String path = location.getPath();
        if (!StringUtils.hasText(path) || !path.contains("/")) {
            throw new IllegalStateException("Keycloak create user response had invalid Location header: " + location);
        }

        String userId = path.substring(path.lastIndexOf('/') + 1);
        if (!StringUtils.hasText(userId)) {
            throw new IllegalStateException("Failed to extract Keycloak user id from Location header: " + location);
        }
        return userId;
    }
}
