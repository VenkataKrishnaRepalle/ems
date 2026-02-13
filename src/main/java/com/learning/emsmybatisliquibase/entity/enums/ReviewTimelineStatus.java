package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ReviewTimelineStatus {
    NOT_STARTED(1),
    SCHEDULED(2),
    STARTED(3),
    OVERDUE(4),
    LOCKED(5),
    COMPLETED(6);

    @EnumValue
    private final int id;
}
