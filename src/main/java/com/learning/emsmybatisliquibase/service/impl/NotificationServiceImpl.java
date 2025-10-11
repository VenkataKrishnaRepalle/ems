package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.ReviewTimelineDao;
import com.learning.emsmybatisliquibase.dto.NotificationDto;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.entity.enums.ReviewType;
import com.learning.emsmybatisliquibase.service.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;

    private final TemplateEngine templateEngine;

    private final ReviewTimelineDao reviewTimelineDao;

    @Value("${default.send.email}")
    String defaultEmail;

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

    @Override
    public void sendSuccessfulEmployeeOnBoard(Employee employee, String password, int capacity) {
        String templateName = capacity == 1 ? emailTemplateNameSuccessfulOnboard :
                getEmailTemplateNameSuccessfulOnboardTempPassword;
        Thread thread = new Thread(() -> {
            try {
                MimeMessageHelper helper = createMimeMessageHelper(defaultEmail, employee.getEmail(),
                        emailTemplateSuccessfulOnboard);

                Context context = new Context();
                context.setVariable("name", employee.getFirstName() + " " + employee.getLastName());
                context.setVariable("email", employee.getEmail());
                context.setVariable("phoneNumber", employee.getPhoneNumber());
                context.setVariable("password", password);

                helper.setText(templateEngine.process(templateName, context), true);
                mailSender.send(helper.getMimeMessage());
            } catch (MessagingException e) {
                log.error("Error sending successful onboarding email for employee with UUID: {}", employee.getUuid(), e);
            }
        });
        thread.start();
    }

    @Override
    public void sendNotificationBeforeStart(List<NotificationDto> notifications, ReviewType reviewType) {
        var thread = new Thread(() -> notifications.forEach(employee -> {
            log.info("Sending notification before start email to colleague {}", employee.getUuid());
            try {
                MimeMessageHelper helper = createMimeMessageHelper(defaultEmail, employee.getEmail(),
                        beforeReviewStartSubject);

                Context context = new Context();
                context.setVariable("name", employee.getFirstName() + " " + employee.getLastName());
                context.setVariable("reviewStartDate", employee.getStartTime());
                context.setVariable("reviewType", reviewType);

                helper.setText(templateEngine.process(beforeReviewStartName, context), true);
                mailSender.send(helper.getMimeMessage());
            } catch (MessagingException e) {
                log.error("Error Sending Notification before start email to colleague {}", employee.getUuid(), e);
            }
        }));
        thread.start();
    }

    @Override
    public void sendStartNotification(ReviewType reviewType) {
        var notifications = reviewTimelineDao.getTimelineIdsByReviewType(reviewType);
        var thread = new Thread(() -> notifications.forEach(employee -> {
            log.info("Sending notification start email to colleague {}", employee.getUuid());
            try {
                MimeMessageHelper helper = createMimeMessageHelper(defaultEmail, employee.getEmail(),
                        reviewStartSubject);

                Context context = new Context();
                context.setVariable("name", employee.getFirstName() + " " + employee.getLastName());
                context.setVariable("reviewStartDate", employee.getStartTime());
                context.setVariable("reviewType", reviewType);

                helper.setText(templateEngine.process(reviewStartName, context), true);
                mailSender.send(helper.getMimeMessage());
            } catch (MessagingException e) {
                log.error("Error Sending Notification before start email to colleague {}", employee.getUuid(), e);
            }
        }));
        thread.start();
    }

    private MimeMessageHelper createMimeMessageHelper(String fromEmail, String toEmail, String subject)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        return helper;
    }
}