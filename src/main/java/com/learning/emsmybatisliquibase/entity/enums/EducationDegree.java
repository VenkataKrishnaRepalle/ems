package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum EducationDegree {
    SSC_10TH(1),
    INTERMEDIATE(2),
    DIPLOMA(3),
    BTECH(4),
    MTECH(5),
    BCA(6),
    BSC(7);

    @EnumValue
    @Getter
    private final int id;
}
