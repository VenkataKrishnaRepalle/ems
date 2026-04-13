package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum OtpAuthType {
    FORGOT_PASSWORD(1),
    RESET_PASSWORD(2),
    VALIDATE_ACCOUNT(3);

    @EnumValue
    private final int id;
}
