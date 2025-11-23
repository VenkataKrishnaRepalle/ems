package com.learning.emsmybatisliquibase.entity;

import com.learning.emsmybatisliquibase.entity.enums.PeriodStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeePeriod implements Serializable {

    private UUID uuid;

    private UUID employeeUuid;

    private UUID periodUuid;

    private PeriodStatus status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
