package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.NotificationDao;
import com.learning.emsmybatisliquibase.entity.Notification;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDao notificationDao;

    private static final int DEFAULT_SIZE = 10;

    @Override
    public Notification getById(UUID id) {
        return notificationDao.getById(id);
    }

    @Override
    public List<Notification> getByEmployee(UUID employeeUuid, List<Notification.Status> statuses, int page) {
       return notificationDao.getByEmployee(employeeUuid, statuses);

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
            if (0 == notificationDao.updateById(uuid, findReverseStatus(status), status)) {
                throw new IntegrityException("NOTIFICATION_UPDATE_FAILED", "Failed to update notification for employee: " + notification.getEmployeeUuid());
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IntegrityException("NOTIFICATION_UPDATE_FAILED", ex.getCause().getMessage());
        }
    }

    @Override
    public void deleteById(UUID id) {
        try {
            if (0 == notificationDao.deleteById(id)) {
                throw new IntegrityException("NOTIFICATION_DELETE_FAILED", "Failed to delete notification for employee: " + id);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IntegrityException("NOTIFICATION_DELETE_FAILED", ex.getCause().getMessage());
        }
    }

    @Override
    public void deleteByEmployee(UUID employeeUuid) {
        try {
            if (0 == notificationDao.deleteByEmployee(employeeUuid)) {
                throw new IntegrityException("NOTIFICATION_DELETE_FAILED", "Failed to delete notification for employee: " + employeeUuid);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IntegrityException("NOTIFICATION_DELETE_FAILED", ex.getCause().getMessage());
        }
    }

    @Override
    public void updateByEmployee(UUID employeeId, Notification.Status status) {
        try {
            if (0 == notificationDao.updateByEmployee(employeeId, findReverseStatus(status), status)) {
                throw new IntegrityException("NOTIFICATION_UPDATE_FAILED", "Failed to update notification for employee: " + employeeId);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IntegrityException("NOTIFICATION_UPDATE_FAILED", ex.getCause().getMessage());
        }
    }

    private Notification.Status findReverseStatus(Notification.Status status) {
        if (Notification.Status.READ.equals(status)) {
            return Notification.Status.UNREAD;
        } else {
            return Notification.Status.READ;
        }
    }
}
