package com.learning.emsmybatisliquibase.entity;

import com.learning.emsmybatisliquibase.entity.enums.EducationDegree;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Education implements Serializable {

    private UUID uuid;

    private UUID employeeUuid;

    private EducationDegree degree;

    private String schoolName;

    private Double grade;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
