package com.learning.emsmybatisliquibase.camunda;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.dao.EmployeeDao;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.entity.Notification;
import com.learning.emsmybatisliquibase.service.NotificationService;
import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.EvaluateDecisionResponse;
import io.camunda.client.api.worker.JobClient;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWorker {

    private final NotificationService notificationService;

    private final CamundaClient camundaClient;

    private final ObjectMapper objectMapper;

    private final EmployeeDao employeeDao;

    @JobWorker(type = "send-welcome-notification")
    public void sendWelcomeNotification(final JobClient client, final ActivatedJob job) {
        var variables = job.getVariablesAsMap();
        var employee = objectMapper.convertValue(variables.get("employee"), Employee.class);
        sendNotification("ONBOARDING", "EMPLOYEE", employee, employee.getUuid());
        client.newCompleteCommand(job.getKey()).send();
    }

    @JobWorker(type = "send-manager-notification")
    public void sendManagerOnboardingNotification(final JobClient client, final ActivatedJob job) {
        var variables = job.getVariablesAsMap();
        var employee = objectMapper.convertValue(variables.get("employee"), Employee.class);

        if (employee.getManagerUuid() != null) {
            var manager = employeeDao.get(employee.getManagerUuid());
            if (manager != null) {
                sendNotification("ONBOARDING", "MANAGER", employee, employee.getManagerUuid());
            }

        }

        client.newCompleteCommand(job.getKey()).send();
    }

    @SneakyThrows
    private void sendNotification(String notificationType, String recipientRole, Employee employeeContext, UUID recipientUuid) {
        Map<String, Object> dmnVariables = Map.of(
                "notificationType", notificationType,
                "recipientRole", recipientRole
        );

        EvaluateDecisionResponse decisionResponse = camundaClient.newEvaluateDecisionCommand()
                .decisionId("notification-decision")
                .variables(dmnVariables)
                .send()
                .join();

        String dmnOutputJson = decisionResponse.getDecisionOutput();

        if (dmnOutputJson != null && !dmnOutputJson.isEmpty()) {
            Map<String, String> dmnResult = objectMapper.readValue(dmnOutputJson, new TypeReference<>() {
            });

            if (!dmnResult.isEmpty()) {
                String titleTemplate = dmnResult.get("title");
                String messageTemplate = dmnResult.get("message");
                String linkTemplate = dmnResult.get("link");

                String title = processTemplate(titleTemplate, employeeContext);
                String message = processTemplate(messageTemplate, employeeContext);
                String link = processTemplate(linkTemplate, employeeContext);

                notificationService.send(Notification.builder()
                        .uuid(UUID.randomUUID())
                        .employeeUuid(recipientUuid)
                        .title(title)
                        .message(message)
                        .link(link)
                        .status(Notification.Status.UNREAD)
                        .createdTime(LocalDateTime.now())
                        .updatedTime(LocalDateTime.now())
                        .build());
            }
        }
        log.info("Notification sent successfully to user: {}", recipientUuid);
    }

    private String processTemplate(String template, Employee employee) {
        if (template == null) {
            return null;
        }
        return template
                .replace("{firstName}", employee.getFirstName())
                .replace("{lastName}", employee.getLastName())
                .replace("{joiningDate}", employee.getJoiningDate() != null ? employee.getJoiningDate().toString() : "N/A")
                .replace("{employeeUuid}", employee.getUuid() != null ? employee.getUuid().toString() : "");
    }
}