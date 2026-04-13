package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.OtpAuth;
import com.learning.emsmybatisliquibase.entity.enums.OtpAuthType;

import java.util.UUID;

public interface OtpService {

    OtpAuth getByUuid(UUID uuid);

    OtpAuth get(RequestQuery requestQuery);

    void sendOtp(String email, OtpAuthType type);

    void verifyOtp(UUID employeeUuid, String otp, OtpAuthType type);

    void update(OtpAuth otpAuth);

    void delete(UUID uuid);
}
