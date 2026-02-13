package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PeriodStatus {
    DRAFT(1),
    INACTIVE(2),
    SCHEDULED(3),
    STARTED(4),
    FAILED(5),
    COMPLETED(6);

    @EnumValue
    private final int id;
}
