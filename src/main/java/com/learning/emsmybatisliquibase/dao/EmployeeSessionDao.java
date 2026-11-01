package com.learning.emsmybatisliquibase.dao;

import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.EmployeeSession;
import com.learning.emsmybatisliquibase.entity.enums.DeviceType;
import com.learning.emsmybatisliquibase.entity.enums.SessionStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

public interface EmployeeSessionDao {
    int insert(@Param("session") EmployeeSession employeeSession);

    EmployeeSession getById(@Param("uuid") UUID uuid);

    EmployeeSession getBySessionId(@Param("sessionId") UUID sessionId, List<SessionStatus> sessionStatusList,
                                   @Param("deviceType") DeviceType deviceType);

    List<EmployeeSession> getByEmployeeUuid(@Param("employeeUuid") UUID employeeUuid);

    int count(@Param("rq") RequestQuery requestQuery);

    int update(@Param("session") EmployeeSession employeeSession);

    int deleteByEmployeeUuid(@Param("employeeUuid") UUID employeeUuid, @Param("device_type") DeviceType deviceType);

    int deleteBySessionId(@Param("sessionId") String employeeUuid);
}
