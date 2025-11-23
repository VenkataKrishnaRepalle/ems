package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.EmployeeDao;
import com.learning.emsmybatisliquibase.dao.OtpDao;
import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.entity.OtpAuth;
import com.learning.emsmybatisliquibase.entity.enums.OtpAuthStatus;
import com.learning.emsmybatisliquibase.entity.enums.OtpAuthType;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.exception.NotFoundException;
import com.learning.emsmybatisliquibase.service.OtpService;
import com.learning.emsmybatisliquibase.utils.ErrorMessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeErrorCodes.EMPLOYEE_NOT_FOUND;
import static com.learning.emsmybatisliquibase.exception.errorcodes.OtpErrorCodes.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final EmployeeDao employeeDao;

    private final OtpDao otpDao;

    @Override
    public OtpAuth getByUuid(UUID uuid) {
        var requestQuery = new RequestQuery();
        requestQuery.setProperty("uuid", uuid);
        var otp = otpDao.get(requestQuery);
        if (1 != otp.size()) {
            log.error(ErrorMessageUtil.getMessage(OTP_NOT_FOUND.code(), uuid));
            throw new NotFoundException(OTP_NOT_FOUND.code(), ErrorMessageUtil.getMessage(OTP_NOT_FOUND.code(), uuid));
        }
        return otp.getFirst();
    }

    @Override
    public OtpAuth get(RequestQuery requestQuery) {
        var otpList = otpDao.get(requestQuery);
        if (1 != otpList.size()) {
            log.error(ErrorMessageUtil.getMessage(OTP_INVALID_SIZED.code(),
                    requestQuery.getPropertyAsString("employeeUuid"), otpList.size()));
            throw new NotFoundException(OTP_INVALID_SIZED.code(),
                    ErrorMessageUtil.getMessage(OTP_INVALID_SIZED.code(),
                            requestQuery.getPropertyAsString("employeeUuid"), otpList.size()));
        }
        return otpList.getFirst();
    }

    @Override
    public OtpAuth sendOtp(String email, OtpAuthType type) {
        var employee = getByEmail(email.trim());
        var requestQuery = createRequestQuery(employee.getUuid(), List.of(type), List.of(OtpAuthStatus.PENDING));

        var existingOtpList = otpDao.get(requestQuery);
        if (!existingOtpList.isEmpty()) {
            existingOtpList.forEach(otp -> {
                otp.setStatus(OtpAuthStatus.EXPIRED);
                otp.setUpdatedTime(LocalDateTime.now());
                update(otp);
            });
        }
        var otpAuth = OtpAuth.builder()
                .uuid(UUID.randomUUID())
                .employeeUuid(employee.getUuid())
                .otp(generateOtp())
                .status(OtpAuthStatus.PENDING)
                .type(type)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();

        try {
            if (0 == otpDao.insert(otpAuth)) {
                log.error(ErrorMessageUtil.getMessage(OTP_SENT_FAILED.code()));
                throw new IntegrityException(OTP_SENT_FAILED.code(), ErrorMessageUtil.getMessage(OTP_SENT_FAILED.code()));
            }
        } catch (DataIntegrityViolationException exception) {
            log.info(exception.getCause().getMessage());
            throw new IntegrityException(OTP_SENT_FAILED.code(), exception.getCause().getMessage());
        }
        return otpAuth;
    }

    private Employee getByEmail(String email) {
        var employee = employeeDao.getByEmail(email.trim());
        if (employee == null) {
            log.error(ErrorMessageUtil.getMessage(EMPLOYEE_NOT_FOUND.code(), email));
            throw new NotFoundException(EMPLOYEE_NOT_FOUND.code(), ErrorMessageUtil.getMessage(EMPLOYEE_NOT_FOUND.code(), email));
        }
        return employee;
    }

    private RequestQuery createRequestQuery(UUID employeeUuid, List<OtpAuthType> types, List<OtpAuthStatus> statuses) {
        var requestQuery = new RequestQuery();
        requestQuery.setProperty("employeeUuid", employeeUuid);
        requestQuery.setProperty("types", types);
        requestQuery.setProperty("statuses", statuses);
        return requestQuery;
    }

    @Override
    public OtpAuth verifyOtp(UUID employeeUuid, String otp, OtpAuthType type) {
        var requestQuery = createRequestQuery(employeeUuid, List.of(type), List.of(OtpAuthStatus.PENDING));

        var existingOtpList = otpDao.get(requestQuery);
        if (existingOtpList.size() != 1) {
            log.error(ErrorMessageUtil.getMessage(OTP_INVALID_SIZED.code(), employeeUuid, existingOtpList.size()));
            throw new IntegrityException(OTP_REGENERATE.code(),
                    ErrorMessageUtil.getMessage(OTP_REGENERATE.code()));
        }

        var otpAuth = existingOtpList.getFirst();

        if (!otp.equals(otpAuth.getOtp().toString())) {
            log.error(ErrorMessageUtil.getMessage(OTP_VERIFICATION_FAILED.code(), employeeUuid));
            throw new InvalidInputException(INVALID_OTP.code(), ErrorMessageUtil.getMessage(OTP_VERIFICATION_FAILED.code()));
        }

        otpAuth.setStatus(OtpAuthStatus.VERIFIED);
        otpAuth.setUpdatedTime(LocalDateTime.now());
        update(otpAuth);
        log.info(ErrorMessageUtil.getMessage(OTP_SUCCESSFUL_VERIFICATION.code(), employeeUuid));

        return otpAuth;
    }

    @Override
    public void delete(UUID uuid) {
        getByUuid(uuid);
        try {
            if (0 == otpDao.delete(uuid)) {
                log.error(ErrorMessageUtil.getMessage(OTP_DELETE_FAILED.code(), uuid));
                throw new IntegrityException(OTP_DELETE_FAILED.code(),
                        ErrorMessageUtil.getMessage(OTP_DELETE_FAILED.code(), uuid));
            }
        } catch (DataIntegrityViolationException e) {
            throw new IntegrityException(OTP_DELETE_FAILED.code(), e.getCause().getMessage());
        }
    }

    public void update(OtpAuth otpAuth) {
        try {
            if (0 == otpDao.update(otpAuth)) {
                log.error(ErrorMessageUtil.getMessage(OTP_UPDATE_FAILED.code(), otpAuth.getEmployeeUuid(), otpAuth.getUuid()));
                throw new IntegrityException(OTP_UPDATE_FAILED.code(),
                        ErrorMessageUtil.getMessage(OTP_UPDATE_FAILED.code(), otpAuth.getUuid()));
            }
        } catch (DataIntegrityViolationException exception) {
            log.info(exception.getCause().getMessage());
            throw new IntegrityException(OTP_UPDATE_FAILED.code(), exception.getCause().getMessage());
        }
    }

    private Long generateOtp() {
        return (long) (100000 + Math.random() * 900000);
    }
}
