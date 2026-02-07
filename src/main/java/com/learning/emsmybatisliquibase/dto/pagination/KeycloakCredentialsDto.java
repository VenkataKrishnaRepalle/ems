package com.learning.emsmybatisliquibase.dto.pagination;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KeycloakCredentialsDto {

    private String type;

    private String value;

    private boolean temporary;
}
