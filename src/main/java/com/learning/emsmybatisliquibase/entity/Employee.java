package com.learning.emsmybatisliquibase.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.learning.emsmybatisliquibase.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalDate;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Employee implements Serializable {

    @Builder.Default
    private UUID uuid = UUID.randomUUID();

    private String firstName;

    private String lastName;

    private Gender gender;

    private LocalDate dateOfBirth;

    private String phoneNumber;

    private String username;

    private String email;

    private UUID managerUuid;

    private Boolean isManager;

    private LocalDate joiningDate;

    private LocalDate leavingDate;

    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime createdTime = LocalDateTime.now();

    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime updatedTime = LocalDateTime.now();
}