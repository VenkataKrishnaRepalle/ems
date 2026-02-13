package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum WorkMode {
    WORK_FROM_HOME(1),
    WORK_FROM_OFFICE(2);

    @EnumValue
    private final int id;
}
