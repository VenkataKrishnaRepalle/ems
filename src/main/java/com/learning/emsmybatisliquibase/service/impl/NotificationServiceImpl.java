package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.NotificationDao;
import com.learning.emsmybatisliquibase.dto.PaginatedResponse;
import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.Notification;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.learning.emsmybatisliquibase.utils.UtilityService.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDao notificationDao;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private static final int DEFAULT_SIZE = 10;

    @Override
    public Notification getById(UUID id) {
        var notification = notificationDao.get(new RequestQuery(Map.of(UUID_NAME, id)));
        if (notification.isEmpty()) {
            return null;
        }
        return notification.getFirst();
    }

    @Override
    public PaginatedResponse<Notification> getByEmployee(UUID employeeUuid, List<Notification.Status> statuses, int page) {
        var totalCount = notificationDao.count(new RequestQuery(
                Map.of(EMPLOYEE_UUID, employeeUuid, STATUSES, statuses)));
        page = Math.max(page, 1);
        RequestQuery request = new RequestQuery();
        request.setSortBy("created_time");
        request.setSize(DEFAULT_SIZE);
        request.setSortOrder("desc");
        request.setOffSet((page - 1) * DEFAULT_SIZE);
        request.setProperty(EMPLOYEE_UUID, employeeUuid);
        request.setProperty(STATUSES, statuses);
        var notifications = notificationDao.get(request);
        return PaginatedResponse.<Notification>builder()
                .currentPage(page)
                .data(notifications.isEmpty() ? null : notifications)
                .totalItems(totalCount > 0 ? totalCount : 0)
                .totalPages(totalCount > 0 ? totalCount / DEFAULT_SIZE : 0)
                .build();
    }

    @Override
    public void send(Notification notification) {
        log.info("Sending notification: {}", notification);
        try {
            int rowsAffected = notificationDao.save(notification);
            if (rowsAffected == 0) {
                log.error("Failed to save notification: {}", notification);
                throw new IntegrityException("NOTIFICATION_INSERT_FAILED", "Failed to save notification for employee: " + notification.getEmployeeUuid());
            } else {
                log.info("Successfully saved notification with ID: {}", notification.getUuid());
                simpMessagingTemplate.convertAndSend("/topic/notifications/" + notification.getEmployeeUuid(), notification);
                simpMessagingTemplate.convertAndSend("/topic/notification/count/" + notification.getEmployeeUuid(), 1);
            }
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity violation while saving notification: {}", notification, ex);
            throw new IntegrityException("NOTIFICATION_INSERT_FAILED", ex.getCause().getMessage());
        }
    }

    @Override
    public void updateById(UUID uuid, Notification.Status status) {
        var notification = getById(uuid);
        try {
            if (0 == notificationDao.update(new RequestQuery(Map.of(UUID_NAME, uuid)),
                    findReverseStatus(status), status, LocalDateTime.now())) {
                throw new IntegrityException("NOTIFICATION_UPDATE_FAILED", "Failed to update notification for employee: " + notification.getEmployeeUuid());
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IntegrityException("NOTIFICATION_UPDATE_FAILED", ex.getCause().getMessage());
        }
        if(notification.isUnread()) {
            simpMessagingTemplate.convertAndSend("/topic/notifications/count/" + notification.getEmployeeUuid(), -1);
        }
    }

    @Override
    public void deleteById(UUID id) {
        var notification = getById(id);
        try {
            if (0 == notificationDao.delete(new RequestQuery(Map.of(UUID_NAME, id)))) {
                throw new IntegrityException("NOTIFICATION_DELETE_FAILED", "Failed to delete notification for employee: " + id);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IntegrityException("NOTIFICATION_DELETE_FAILED", ex.getCause().getMessage());
        }
        if(notification.isUnread()) {
            simpMessagingTemplate.convertAndSend("/topic/notifications/count/" + notification.getEmployeeUuid(), -1);
        }
    }

    @Override
    public void deleteByEmployee(UUID employeeUuid) {
        try {
            if (0 == notificationDao.delete(new RequestQuery(Map.of(EMPLOYEE_UUID, employeeUuid)))) {
                throw new IntegrityException("NOTIFICATION_DELETE_FAILED", "Failed to delete notification for employee: " + employeeUuid);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IntegrityException("NOTIFICATION_DELETE_FAILED", ex.getCause().getMessage());
        }
    }

    @Override
    public void updateByEmployee(UUID employeeId, Notification.Status status) {
        try {
            if (0 == notificationDao.update(new RequestQuery(Map.of(EMPLOYEE_UUID, employeeId)),
                    findReverseStatus(status), status, LocalDateTime.now())) {
                throw new IntegrityException("NOTIFICATION_UPDATE_FAILED", "Failed to update notification for employee: " + employeeId);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IntegrityException("NOTIFICATION_UPDATE_FAILED", ex.getCause().getMessage());
        }
    }

    @Override
    public Map<String, Long> getCount(UUID employeeUuid) {
        List<Map<String, Object>> counts = notificationDao.getCount(employeeUuid);
        return counts.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("status"),
                        m -> (Long) m.get("count")
                ));
    }

    private Notification.Status findReverseStatus(Notification.Status status) {
        if (Notification.Status.READ.equals(status)) {
            return Notification.Status.UNREAD;
        } else {
            return Notification.Status.READ;
        }
    }
}
