package com.learning.emsmybatisliquibase.entity.camunda;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ProcessExecutionStatus {
    IN_PROGRESS(1),
    COMPLETED(2),
    FAILED(3),
    COMPENSATED(4),
    CANCELLED(5);

    @EnumValue
    @Getter
    private final int id;
}
