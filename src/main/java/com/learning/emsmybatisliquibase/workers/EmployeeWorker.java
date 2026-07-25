package com.learning.emsmybatisliquibase.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import com.learning.emsmybatisliquibase.service.KeycloakService;
import com.learning.emsmybatisliquibase.service.ProcessExecutionService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeWorker {

    private final EmployeeService employeeService;

    private final KeycloakService keycloakService;

    private final ProcessExecutionService processExecutionService;

    private final ObjectMapper objectMapper;

    @JobWorker(type = "save-employee-data", autoComplete = true)
    public void saveEmployeeData(final JobClient client, final ActivatedJob job) {
        var data = job.getVariablesAsMap();
        if (!data.containsKey("employee")) {
            throw new InvalidInputException("INVALID_INPUT", "invalid input");
        }
        Employee employee = objectMapper.convertValue(data.get("employee"), Employee.class);
        try {
            employee = employeeService.insert(employee);
            log.info("Employee onboarded successfully with id: {}", employee.getUuid());
        } catch (Exception e) {
            client.newThrowErrorCommand(job.getKey())
                    .errorCode("compensate-delete-keycloak-user")
                    .errorMessage("Employee onboarding failed for email: " + employee.getEmail())
                    .send();
            processExecutionService.updateErrorDetails(job.getProcessInstanceKey(),
                    "save-employee-data",
                    "EMPLOYEE_INSERT_FAILED",
                    "Failed to create employee: " + employee.getEmail());
        }
    }

    @JobWorker(type = "compensate-keycloak-employee-error")
    public void compensateKeycloakEmployeeError(final ActivatedJob job) {
        var data = job.getVariablesAsMap();
        if (!data.containsKey("employeeUuid")) {
            throw new IntegrityException("INVALID_INPUT", "Invalid input: employeeUuid");
        }

        UUID employeeUuid = objectMapper.convertValue(data.get("employeeUuid"), UUID.class);
        employeeService.delete(employeeUuid);
        keycloakService.delete(employeeUuid);
    }
}
