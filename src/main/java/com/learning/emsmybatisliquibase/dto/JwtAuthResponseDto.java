package com.learning.emsmybatisliquibase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtAuthResponseDto {

    private UUID employeeId;

    private String email;

    private String accessToken;

    private String refreshToken;

    private String tokenType = "Bearer";

    private List<String> roles;
}
