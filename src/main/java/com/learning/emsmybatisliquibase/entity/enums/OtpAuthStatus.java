package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum OtpAuthStatus {
    PENDING(1),
    VERIFIED(2),
    EXPIRED(3);

    @EnumValue
    private final int id;
}
