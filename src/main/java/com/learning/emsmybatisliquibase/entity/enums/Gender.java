package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Gender {
    MALE(1),
    FEMALE(2),
    OTHERS(3);

    @EnumValue
    @Getter
    private final int id;
}
