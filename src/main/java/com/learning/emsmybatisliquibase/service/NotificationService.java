package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.dto.PaginatedResponse;
import com.learning.emsmybatisliquibase.entity.Notification;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationService {
    Notification getById(UUID id);

    PaginatedResponse<Notification> getByEmployee(UUID employeeUuid, List<Notification.Status> statuses, int page);

    void send(Notification notification);

    void updateById(UUID id, Notification.Status status);

    void deleteById(UUID id);

    void deleteByEmployee(UUID employeeUuid);

    void updateByEmployee(UUID employeeId, Notification.Status status);

    Map<String, Long> getCount(UUID employeeUuid);
}
