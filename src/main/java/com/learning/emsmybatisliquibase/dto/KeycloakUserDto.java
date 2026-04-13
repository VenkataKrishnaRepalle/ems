package com.learning.emsmybatisliquibase.dto;

import com.learning.emsmybatisliquibase.dto.pagination.KeycloakCredentialsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KeycloakUserDto {
    private String id;

    private String username;

    private String email;

    private String firstName;

    private String lastName;

    private boolean enabled;

    private boolean emailVerified;

    private List<KeycloakCredentialsDto> credentials;
}
