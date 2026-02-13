package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ReviewType {
    Q1(1),
    Q2(2),
    Q3(3),
    Q4(4);

    @EnumValue
    private final int id;
}
