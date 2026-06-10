package com.learning.emsmybatisliquibase.camunda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import com.learning.emsmybatisliquibase.service.KeycloakService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmployeeWorker {

    private final EmployeeService employeeService;

    private final KeycloakService keycloakService;

    private final ObjectMapper objectMapper;

    @JobWorker(type = "save-employee-data", autoComplete = true)
    public void saveEmployeeData(final ActivatedJob job) {
        var data = job.getVariablesAsMap();
        if (!data.containsKey("employee")) {
            throw new InvalidInputException("INVALID_INPUT", "invalid input");
        }
        Employee employee = objectMapper.convertValue(data.get("employee"), Employee.class);
        employeeService.insert(employee);
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
