package com.learning.emsmybatisliquibase.camunda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.service.CommunicationService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunicationWorker {

    private final CommunicationService communicationService;

    private final ObjectMapper objectMapper;

    @JobWorker(type = "send-onboarding-communication")
    public void sendOnboardingCommunication(final JobClient client, final ActivatedJob job) {
        var data = job.getVariablesAsMap();
        if (!data.containsKey("employee")) {
            throw new InvalidInputException("INVALID_INPUT", "Not a valid input: employee");
        }
        if (!data.containsKey("password")) {
            throw new InvalidInputException("INVALID_INPUT", "Not a valid input: password");
        }
        Employee employee = objectMapper.convertValue(data.get("employee"), Employee.class);
        String password = objectMapper.convertValue(data.get("password"), String.class);
        communicationService.sendSuccessfulEmployeeOnBoard(employee, password);

        log.info("Notification send successfully to user: {}", employee.getUuid());
//        client.newCompleteCommand(job.getKey()).send();
    }
}