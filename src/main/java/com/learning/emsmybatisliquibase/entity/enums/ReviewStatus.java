package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ReviewStatus {
    NOT_SUBMITTED(1),
    DRAFT(2),
    WAITING_FOR_APPROVAL(3),
    APPROVED(4),
    DECLINED(5),
    COMPLETED(6);

    @EnumValue
    private final int id;
}
