package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.dto.NotificationDto;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.entity.enums.ReviewType;

import java.util.List;

public interface CommunicationService {

    void sendSuccessfulEmployeeOnBoard(Employee employee, String password, int capacity);

    void sendNotificationBeforeStart(List<NotificationDto> notifications, ReviewType reviewType);

    void sendStartNotification(ReviewType startedReviewType);
}
