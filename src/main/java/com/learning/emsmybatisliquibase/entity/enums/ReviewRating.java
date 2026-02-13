package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public enum ReviewRating {
    NEW_TO_BUSINESS(1, List.of(0)), // 0
    BELOW_EXPECTED(2, List.of(1, 2, 3)), // 1-3
    EXPECTED(3, List.of(4, 5, 6)), // 4-6
    BROADLY_IN_LINE(4, List.of(7, 8)), //7-8
    GREAT(5, List.of(9)), //9
    OUTSTANDING(6, List.of(10)); //10

    @EnumValue
    private final int id;
    private final List<Integer> rating;

//    NEW TO BUSINESS: 0
//BELOW EXPECTED: 1
//BELOW EXPECTED: 2
//BELOW EXPECTED: 3
//EXPECTED: 4
//EXPECTED: 5
//EXPECTED: 6
//BROADLY IN LINE: 7
//BROADLY IN LINE: 8
//GREAT: 9
//OUTSTANDING: 10
}
