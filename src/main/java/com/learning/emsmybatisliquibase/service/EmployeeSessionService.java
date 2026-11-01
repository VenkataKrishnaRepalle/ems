package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.EmployeeSession;
import com.learning.emsmybatisliquibase.entity.enums.DeviceType;
import com.learning.emsmybatisliquibase.entity.enums.SessionStatus;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.UUID;

public interface EmployeeSessionService {

    EmployeeSession insert(HttpServletRequest request, String sessionIdHash, UUID employeeUuid);

    int count(RequestQuery requestQuery);

    EmployeeSession getBySessionId(String sessionId, List<SessionStatus> sessionStatuses, DeviceType deviceType);

    EmployeeSession update(UUID employeeUuid, EmployeeSession employeeSession);

    void deleteBySessionId(String sessionUuid);

    void deleteByEmployeeUuid(UUID employeeUuid, DeviceType deviceType);
}
