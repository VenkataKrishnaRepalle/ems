package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.entity.EmployeeSession;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EmployeeSessionService {
    Map<String, List<EmployeeSession>> get(String email, Boolean isActive);

    EmployeeSession update(UUID employeeUuid, EmployeeSession employeeSession);

    void delete(UUID sessionUuid);
}
