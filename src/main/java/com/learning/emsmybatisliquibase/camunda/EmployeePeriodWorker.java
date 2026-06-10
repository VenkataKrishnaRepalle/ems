package com.learning.emsmybatisliquibase.camunda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.service.EmployeePeriodService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmployeePeriodWorker {

    private final EmployeePeriodService employeePeriodService;

    private final ObjectMapper objectMapper;

    @JobWorker(type = "assign-employee-period")
    public void assignEmployeePeriod(final ActivatedJob job) {
        var data = job.getVariablesAsMap();

        if (!data.containsKey("employeeUuid")) {
            throw new InvalidInputException("INVALID_INPUT", "invalid input: employeeUuid");
        }

        UUID employeeUuid = objectMapper.convertValue(data.get("employeeUuid"), UUID.class);
        employeePeriodService.periodAssignment(List.of(employeeUuid));
    }
}
