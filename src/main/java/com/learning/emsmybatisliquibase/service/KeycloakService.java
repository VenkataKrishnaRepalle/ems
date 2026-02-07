package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.dto.KeycloakCreateUserDto;

public interface KeycloakService {

    void create(KeycloakCreateUserDto dto);
}
