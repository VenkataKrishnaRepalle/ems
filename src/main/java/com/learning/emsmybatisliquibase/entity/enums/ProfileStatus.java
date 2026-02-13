package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ProfileStatus {
    ACTIVE(1),
    PENDING(2),
    INACTIVE(3);

    @EnumValue
    private final int id;
}
