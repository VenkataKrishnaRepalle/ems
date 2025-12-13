package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.dto.EmployeeCycleAndTimelineResponseDto;
import com.learning.emsmybatisliquibase.dto.FullEmployeePeriodDto;
import com.learning.emsmybatisliquibase.dto.SuccessResponseDto;
import com.learning.emsmybatisliquibase.entity.enums.PeriodStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface EmployeePeriodService {
    SuccessResponseDto periodAssignment(List<UUID> employeeIds);

    SuccessResponseDto updateEmployeePeriodStatus(UUID employeePeriodId, PeriodStatus status);

    void updateEmployeePeriodsByPeriodId(UUID periodId, PeriodStatus status);

    FullEmployeePeriodDto getEmployeePeriodById(UUID employeePeriodId);

    EmployeeCycleAndTimelineResponseDto getByEmployeeIdAndPeriodId(UUID employeeId, UUID periodId);

    Map<String, EmployeeCycleAndTimelineResponseDto> getAll(UUID employeeId);

    EmployeeCycleAndTimelineResponseDto getByYear(UUID employeeId, Optional<Long> year);

    List<Integer> getAllEligibleYears(UUID employeeId);

    SuccessResponseDto reprocess(List<UUID> employeeIds, Optional<Long> year);
}
