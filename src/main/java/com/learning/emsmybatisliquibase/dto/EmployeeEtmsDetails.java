package com.learning.emsmybatisliquibase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeEtmsDetails {
    private String employeeUuid;

    private String name;

    private String email;

    private String phone;

    private UUID managerUuid;

    private String managerFullName;

    private String managerPhone;

    private String managerEmail;

    private String home_latitude;

    private String home_longitude;

    private Instant createdTime;

    private Instant updatedTime;
}
