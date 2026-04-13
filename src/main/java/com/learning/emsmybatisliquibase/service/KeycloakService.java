package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.dto.KeycloakUserDto;

public interface KeycloakService {

    void create(KeycloakUserDto dto);

    void update(KeycloakUserDto dto);
}
