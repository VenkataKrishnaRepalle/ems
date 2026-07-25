package com.learning.emsmybatisliquibase.entity;

import com.learning.emsmybatisliquibase.dto.AddEmployeeDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeBatchOnboarding {

    private UUID id;

    private String importId;

    private Integer batchNo;

    private List<AddEmployeeDto> employees;

    private int totalCount;

    private String status;

    private UUID createdBy;

    private LocalDateTime createdAt;
}
