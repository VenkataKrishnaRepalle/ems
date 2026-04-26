package com.learning.emsmybatisliquibase.service;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public interface KeycloakService {

    String create(UserRepresentation dto);

    String create(UserRepresentation dto, List<String> roles);

    void update(UserRepresentation dto);

    void update(UserRepresentation dto, List<String> roles);
}
