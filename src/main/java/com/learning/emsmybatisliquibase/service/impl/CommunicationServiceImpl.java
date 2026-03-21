package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.ReviewTimelineDao;
import com.learning.emsmybatisliquibase.dto.EmailRecipientDetailsDto;
import com.learning.emsmybatisliquibase.dto.EmailRequestDto;
import com.learning.emsmybatisliquibase.dto.EmailResponseDto;
import com.learning.emsmybatisliquibase.dto.NotificationDto;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.entity.enums.ReviewType;
import com.learning.emsmybatisliquibase.service.CommunicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunicationServiceImpl implements CommunicationService {

    private final TemplateEngine templateEngine;

    private final ReviewTimelineDao reviewTimelineDao;

    @Value("${default.send.email.address}")
    String defaultEmail;

    @Value("${default.send.email.header}")
    private String emailHeader;

    @Value("${default.send.email.api-key}")
    private String apiKey;

    @Value("${default.send.email.url}")
    private String baseUrl;

    @Value("${email.template.successful-onboard.name}")
    String emailTemplateNameSuccessfulOnboard;

    @Value("${email.template.successful-onboard.temp-password.name}")
    String getEmailTemplateNameSuccessfulOnboardTempPassword;

    @Value("${email.template.successful-onboard.subject}")
    String emailTemplateSuccessfulOnboard;

    @Value("${email.template.review.start.before.name}")
    String beforeReviewStartName;

    @Value("${email.template.review.start.before.subject}")
    String beforeReviewStartSubject;

    @Value("${email.template.review.start.name}")
    String reviewStartName;

    @Value("${email.template.review.start.subject}")
    String reviewStartSubject;

    private WebClient webClient;

    @PostConstruct
    void initWebClient() {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public void sendSuccessfulEmployeeOnBoard(Employee employee, String password, int capacity) {
        String templateName = capacity == 1 ? emailTemplateNameSuccessfulOnboard :
                getEmailTemplateNameSuccessfulOnboardTempPassword;
        Thread thread = new Thread(() -> {
            String fullName = employee.getFirstName() + " " + employee.getLastName();
            Context context = new Context();
            context.setVariable("name", fullName);
            context.setVariable("email", employee.getEmail());
            context.setVariable("phoneNumber", employee.getPhoneNumber());
            context.setVariable("password", password);

            var htmlContent = templateEngine.process(templateName, context);
            var emailRequestDto = constructEmailRequest(fullName, employee.getEmail(), emailTemplateSuccessfulOnboard, htmlContent);
            sendEmail(emailRequestDto);
        });
        thread.start();
    }

    @Override
    public void sendNotificationBeforeStart(List<NotificationDto> notifications, ReviewType reviewType) {
        var thread = new Thread(() -> notifications.forEach(employee -> {
            log.info("Sending notification before start email to colleague {}", employee.getUuid());
            var fullName = employee.getFirstName() + " " + employee.getLastName();
            Context context = new Context();
            context.setVariable("name", fullName);
            context.setVariable("reviewStartDate", employee.getStartTime());
            context.setVariable("reviewType", reviewType);

            var htmlContent = templateEngine.process(beforeReviewStartName, context);
            var emailRequest = constructEmailRequest(fullName, employee.getEmail(), beforeReviewStartSubject, htmlContent);
            sendEmail(emailRequest);
        }));
        thread.start();
    }

    @Override
    public void sendStartNotification(ReviewType reviewType) {
        var notifications = reviewTimelineDao.getTimelineIdsByReviewType(reviewType);
        var thread = new Thread(() -> notifications.forEach(employee -> {
            log.info("Sending notification start email to colleague {}", employee.getUuid());
            var fullName = employee.getFirstName() + " " + employee.getLastName();
            Context context = reviewStartContext(fullName, employee.getStartTime(), reviewType);
            var htmlContent = templateEngine.process(reviewStartName, context);
            var emailRequest = constructEmailRequest(fullName, employee.getEmail(), reviewStartSubject, htmlContent);
            sendEmail(emailRequest);
        }));
        thread.start();
    }

    private Context reviewStartContext(String name, Object startDate, ReviewType reviewType) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("reviewStartDate", startDate);
        context.setVariable("reviewType", reviewType);
        return context;
    }

    private EmailRequestDto constructEmailRequest(String toName, String toEmail, String subject, String htmlContent) {
        return EmailRequestDto.builder()
                .sender(EmailRecipientDetailsDto.builder()
                        .email(defaultEmail)
                        .name(emailHeader)
                        .build())
                .to(List.of(EmailRecipientDetailsDto.builder()
                        .email(toEmail)
                        .name(toName)
                        .build()))
                .subject(subject)
                .htmlContent(htmlContent)
                .build();
    }

    private void sendEmail(EmailRequestDto emailRequestDto) {
        webClient.post()
                .uri("/email")
                .header("api-key", apiKey)
                .bodyValue(emailRequestDto)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    log.error("Error fetching Location: {}", clientResponse.statusCode());
                    return clientResponse.createException().flatMap(Mono::error);
                })
                .bodyToMono(EmailResponseDto.class)
                .block();

    }
}
