package com.learning.emsmybatisliquibase.service;

import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.UUID;

public interface KeycloakService {

    String create(UserRepresentation dto);

    String create(UserRepresentation dto, List<String> roles);

    void update(UserRepresentation dto);

    void update(UserRepresentation dto, List<String> roles);

    void delete(UUID uuid);
}
