package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PasswordStatus {
    ACTIVE(1),
    EXPIRED(2),
    LOCKED(3);

    @EnumValue
    private final int id;
}
