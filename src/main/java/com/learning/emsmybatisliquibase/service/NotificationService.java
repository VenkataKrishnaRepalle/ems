package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.entity.Notification;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    Notification getById(UUID id);

    List<Notification> getByEmployee(UUID employeeUuid, List<Notification.Status> statuses, int page);

    void send(Notification notification);

    void updateById(UUID id, Notification.Status status);

    void deleteById(UUID id);

    void deleteByEmployee(UUID employeeUuid);

    void updateByEmployee(UUID employeeId, Notification.Status status);
}
