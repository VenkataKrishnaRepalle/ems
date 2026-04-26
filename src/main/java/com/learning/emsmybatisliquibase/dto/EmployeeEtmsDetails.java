package com.learning.emsmybatisliquibase.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeEtmsDetails {
    private UUID uuid;

    private String name;

    private String email;

    private String phone;

    private String profileStatus;

    private UUID managerUuid;

    private String managerFullName;

    private String managerPhone;

    private String managerEmail;

    private Instant createdTime;

    private Instant updatedTime;
}
