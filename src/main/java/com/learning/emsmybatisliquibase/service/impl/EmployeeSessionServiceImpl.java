package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.EmployeeSessionDao;
import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.EmployeeSession;
import com.learning.emsmybatisliquibase.entity.enums.DeviceType;
import com.learning.emsmybatisliquibase.entity.enums.SessionStatus;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.service.EmployeeSessionService;
import com.learning.emsmybatisliquibase.utils.ErrorMessageUtil;
import com.learning.emsmybatisliquibase.utils.HashUtil;
import com.learning.emsmybatisliquibase.utils.UtilityService;
import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeSessionErrorCodes.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeSessionServiceImpl implements EmployeeSessionService {

    private final EmployeeSessionDao employeeSessionDao;

    @Override
    public EmployeeSession insert(HttpServletRequest request, String sessionIdHash, UUID employeeUuid) {
        var userAgentString = request.getHeader("User-Agent");
        var userAgent = UserAgent.parseUserAgentString(userAgentString);
        log.info("User Agent String: {}", userAgentString);
        log.info("User Agent: {}", userAgent);
        var deviceType = UtilityService.getDeviceDetails(request);
        log.info("Device Type: {}", deviceType);

        deleteByEmployeeUuid(employeeUuid, deviceType);

        EmployeeSession session = EmployeeSession.builder()
                .uuid(UUID.randomUUID())
                .employeeUuid(employeeUuid)
                .sessionIdHash(sessionIdHash)
                .userAgentHash(HashUtil.hash(userAgentString))
                .status(SessionStatus.ACTIVE)
                .deviceType(deviceType)
                .ipAddress(request.getRemoteAddr())
                .lastAccessedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now())
                .build();
        try {
            if (0 == employeeSessionDao.insert(session)) {
                throw new IntegrityException("EMPLOYEE_SESSION_NOT_INSERTED",
                        "Employee Session not created for employee : " + employeeUuid);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("EMPLOYEE_SESSION_NOT_INSERTED", exception.getCause().getMessage());
        }
        return session;
    }

    @Override
    public int count(RequestQuery requestQuery) {
        return employeeSessionDao.count(requestQuery);
    }

    @Override
    public EmployeeSession getBySessionId(String sessionId, List<SessionStatus> sessionStatuses, DeviceType deviceType) {
        return null;
    }

    @Override
    public EmployeeSession update(UUID employeeUuid, EmployeeSession employeeSession) {
        var existingSession = employeeSessionDao.getById(employeeSession.getUuid());
        if (null == existingSession) {
            throw new NotFoundException(EMPLOYEE_SESSION_NOT_FOUND.code(),
                    ErrorMessageUtil.getMessage(EMPLOYEE_SESSION_NOT_FOUND.code(), employeeSession.getUuid()));
        }
        try {
            if (0 == employeeSessionDao.update(employeeSession)) {
                throw new IntegrityException(EMPLOYEE_SESSION_UPDATE_FAILED.code(),
                        ErrorMessageUtil.getMessage(EMPLOYEE_SESSION_UPDATE_FAILED.code(), employeeSession.getUuid()));
            }
        } catch (DataIntegrityViolationException e) {
            throw new IntegrityException(EMPLOYEE_SESSION_UPDATE_FAILED.code(),
                    e.getCause().getMessage());
        }
        return employeeSession;
    }

    @Override
    public void deleteBySessionId(String sessionUuid) {
        try {
            employeeSessionDao.deleteBySessionId(sessionUuid);
        } catch (DataIntegrityViolationException e) {
            throw new IntegrityException(EMPLOYEE_SESSION_DELETE_FAILED.code(),
                    e.getCause().getMessage());
        }
    }

    public void deleteByEmployeeUuid(UUID employeeUuid, DeviceType deviceType) {
        try {
            employeeSessionDao.deleteByEmployeeUuid(employeeUuid, deviceType);
        } catch (DataIntegrityViolationException e) {
            throw new IntegrityException(EMPLOYEE_SESSION_DELETE_FAILED.code(),
                    e.getCause().getMessage());
        }
    }
}
