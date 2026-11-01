package com.learning.emsmybatisliquibase.entity;

import com.learning.emsmybatisliquibase.entity.enums.DeviceType;
import com.learning.emsmybatisliquibase.entity.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeSession {

    private UUID uuid;

    private UUID employeeUuid;

    private String sessionIdHash;

    private String userAgentHash;

    private SessionStatus status;

    private DeviceType deviceType;

    private String ipAddress;

    private LocalDateTime lastAccessedAt;

    private LocalDateTime expiredAt;

    private LocalDateTime createdAt;
}
