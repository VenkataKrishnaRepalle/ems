package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum CertificationLevel {
    BEGINNER(1),
    INTERMEDIATE(2),
    EXPERT(3);

    @EnumValue
    private final int id;
}