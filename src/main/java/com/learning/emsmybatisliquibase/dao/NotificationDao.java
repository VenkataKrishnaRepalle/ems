package com.learning.emsmybatisliquibase.dao;

import com.learning.emsmybatisliquibase.entity.Notification;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationDao {

    Notification getById(@Param("id") UUID id);

    List<Notification> getByEmployee(@Param("employeeUuid") UUID employeeUuid, @Param("statuses") List<Notification.Status> statuses);

    int save(@Param("notification")Notification notification);

    int updateById(@Param("id") UUID id, @Param("oldStatus") Notification.Status oldStatus, @Param("newStatus") Notification.Status newStatus);

    int updateByEmployee(@Param("employeeUuid") UUID employeeUuid, @Param("oldStatus") Notification.Status oldStatus, @Param("newStatus") Notification.Status newStatus);

    int deleteById(@Param("id") UUID id);

    int deleteByEmployee(@Param("employeeUuid") UUID employeeUuid);
}