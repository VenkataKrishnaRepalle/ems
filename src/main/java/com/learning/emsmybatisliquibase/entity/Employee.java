package com.learning.emsmybatisliquibase.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.learning.emsmybatisliquibase.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Employee implements Serializable {

    private UUID uuid;

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

    @JsonIgnore
    private Instant createdTime;

    @JsonIgnore
    private Instant updatedTime;
}