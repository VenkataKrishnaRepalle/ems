package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AttendanceStatus {
    SUBMITTED(1),
    APPROVED(2),
    CANCELLED(3);

    @EnumValue
    private final int id;
}
