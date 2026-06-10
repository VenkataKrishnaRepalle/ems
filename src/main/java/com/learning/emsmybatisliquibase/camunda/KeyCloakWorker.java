package com.learning.emsmybatisliquibase.camunda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.service.KeycloakService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeyCloakWorker {

    private final KeycloakService keycloakService;

    private final ObjectMapper objectMapper;

    private static final String EMPLOYEE = "employee";

    @JobWorker(type = "create-keycloak-user")
    public void createKeycloakUser(final JobClient client, final ActivatedJob job) {
        var data = job.getVariablesAsMap();
        if (!data.containsKey(EMPLOYEE)) {
            throw new InvalidInputException("INVALID_INPUT", "Not a valid input: employee");
        }
        if (!data.containsKey("password")) {
            throw new InvalidInputException("INVALID_INPUT", "Not a valid input: password");
        }
        Employee employee = objectMapper.convertValue(data.get(EMPLOYEE), Employee.class);
        String password = (String) data.get("password");

        List<String> roles = new ArrayList<>(List.of("EMPLOYEE"));
        if (Boolean.TRUE.equals(employee.getIsManager())) {
            roles.add("MANAGER");
        }
        String uuid = keycloakService.create(getUserRepresentation(employee, password), roles);
        UUID employeeUuid = UUID.fromString(uuid);
        employee.setUuid(employeeUuid);

        Map<String, Object> variables = new HashMap<>();
        variables.put(EMPLOYEE, employee);
        variables.put("employeeUuid", employeeUuid.toString());

        client.newCompleteCommand(job.getKey())
                .variables(variables)
                .send()
                .join();
    }

    private static @NonNull UserRepresentation getUserRepresentation(Employee employee, String password) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(employee.getUsername());
        userRepresentation.setEmail(employee.getEmail());
        userRepresentation.setFirstName(employee.getFirstName());
        userRepresentation.setLastName(employee.getLastName());
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(true);
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setTemporary(false);
        credentialRepresentation.setValue(password);
        userRepresentation.setCredentials(List.of(credentialRepresentation));
        return userRepresentation;
    }

    @JobWorker(type = "compensate-delete-keycloak-user")
    public void compensateDeleteKeycloakUser(final ActivatedJob job) {
        var data = job.getVariablesAsMap();
        if (!data.containsKey("employeeUuid")) {
            throw new InvalidInputException("INVALID_INPUT", "Not a valid input: employeeUuid");
        }
        UUID employeeUuid = objectMapper.convertValue(data.get("employeeUuid"), UUID.class);
        log.info("compensateDeleteKeycloakUser: Deleting user with id: {}", employeeUuid);

        keycloakService.delete(employeeUuid);
    }

}