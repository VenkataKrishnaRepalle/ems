package com.learning.emsmybatisliquibase.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.service.EmployeePeriodService;
import com.learning.emsmybatisliquibase.service.ProcessExecutionService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeePeriodWorker {

    private final EmployeePeriodService employeePeriodService;

    private final ProcessExecutionService processExecutionService;

    private final ObjectMapper objectMapper;

    @JobWorker(type = "assign-employee-period")
    public void assignEmployeePeriod(final JobClient client, final ActivatedJob job) {
        var data = job.getVariablesAsMap();

        if (!data.containsKey("employeeUuid")) {
            throw new InvalidInputException("INVALID_INPUT", "invalid input: employeeUuid");
        }

        UUID employeeUuid = objectMapper.convertValue(data.get("employeeUuid"), UUID.class);
        try {
            employeePeriodService.periodAssignment(List.of(employeeUuid));
            log.info("Employee Periods assigned for employee: {}", employeeUuid);
        } catch (Exception e) {
            client.newThrowErrorCommand(job.getKey())
                    .errorCode("compensate-keycloak-employee-error")
                    .errorMessage("Employee assignment failed for employee: " + employeeUuid)
                    .send()
                    .join();

            processExecutionService.updateErrorDetails(job.getProcessInstanceKey(),
                    "assign-employee-period",
                    "EMPLOYEE_PERIOD_ASSIGNMENT_FAILED",
                    "Failed to assign employee period for employee: " + employeeUuid);
        }
    }
}
