package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AttendanceType {
    HALF_DAY(1),
    FULL_DAY(2);

    @EnumValue
    private final int id;
}
