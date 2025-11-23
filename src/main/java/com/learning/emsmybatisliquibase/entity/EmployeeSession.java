package com.learning.emsmybatisliquibase.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeSession {

    private UUID uuid;

    private UUID employeeUuid;

    private String token;

    private String latitude;

    private String longitude;

    private String browserName;

    private String osName;

    private String platform;

    private String location;

    private Boolean isActive;

    private LocalDateTime loginTime;
}
