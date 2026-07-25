package com.learning.emsmybatisliquibase.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.emsmybatisliquibase.dao.EmployeeDao;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.entity.Notification;
import com.learning.emsmybatisliquibase.service.NotificationService;
import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
        Map<String, String> dmnResult = objectMapper.convertValue(variables.get("dmnResult"), Map.class);

        String title = processTemplate(dmnResult.get("title"), employee);
        String message = processTemplate(dmnResult.get("message"), employee);
        
        if(title == null || message == null) {
            log.error("Invalid decision result. Failed to send onboarding notification to employee: {}", employee.getUuid());
            return;
        }

        sendNotification(title, message, null, employee.getUuid());
    }

    @JobWorker(type = "send-manager-notification")
    public void sendManagerOnboardingNotification(final JobClient client, final ActivatedJob job) {
        var variables = job.getVariablesAsMap();
        var employee = objectMapper.convertValue(variables.get("employee"), Employee.class);

        if (employee.getManagerUuid() != null) {
            Map<String, String> dmnResult = objectMapper.convertValue(variables.get("dmnResult"), Map.class);

            String title = processTemplate(dmnResult.get("title"), employee);
            String message = processTemplate(dmnResult.get("message"), employee);
            String link = processTemplate(dmnResult.get("link"), employee);

            if(title == null || message == null || link == null) {
                log.error("Invalid decision result. Failed to send onboarding notification to manager: {}", employee.getUuid());
                return;
            }

            sendNotification(title, message, link, employee.getManagerUuid());
        }
    }

    @SneakyThrows
    private void sendNotification(String title, String message, String link, UUID recipientUuid) {
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