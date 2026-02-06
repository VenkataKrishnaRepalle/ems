package com.learning.emsmybatisliquibase.dao;

import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.Notification;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationDao {
    int save(@Param("notification") Notification notification);

    Long count(@Param("request") RequestQuery requestQuery);

    int update(@Param("request") RequestQuery requestQuery, @Param("oldStatus") Notification.Status oldStatus, @Param("newStatus") Notification.Status newStatus, @Param("updatedTime") LocalDateTime updatedTime);

    int delete(@Param("request") RequestQuery requestQuery);

    List<Notification> get(@Param("request") RequestQuery requestQuery);

    List<Map<String, Object>> getCount(@Param("employeeUuid") UUID employeeUuid);
}