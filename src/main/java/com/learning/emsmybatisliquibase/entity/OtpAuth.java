package com.learning.emsmybatisliquibase.entity;

import com.learning.emsmybatisliquibase.entity.enums.OtpAuthStatus;
import com.learning.emsmybatisliquibase.entity.enums.OtpAuthType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OtpAuth implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID uuid;

    private UUID employeeUuid;

    private Long otp;

    private OtpAuthStatus status;

    private OtpAuthType type;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
