package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LeaveStatus {
    WAITING_FOR_APPROVAL(1),
    APPROVED(2),
    DECLINED(3),
    CANCELLED(4);

    @EnumValue
    @Getter
    private final int id;
}
