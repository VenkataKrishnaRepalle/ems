package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum JobTitleType {
    ENGINEER_TRAINEE(1, "Engineer Trainee"),
    SOFTWARE_ENGINEER(2, "Software Engineer"),
    SENIOR_SOFTWARE_ENGINEER(3, "Senior Software Engineer"),
    MODULE_LEAD(4, "Module Lead"),
    TECHNICAL_LEAD(5, "Technical Lead"),
    PROJECT_LEAD(6, "Project Lead"),
    PROJECT_MANAGER(7, "Project Manager"),
    SENIOR_PROJECT_MANAGER(8, "Senior Project Manager"),
    PRINCIPAL_DELIVERY_MANAGER(9, "Principal Delivery Manager"),
    ASSOCIATIVE_DIRECTOR(10, "Associative Director"),
    SENIOR_DIRECTOR(11, "Senior Director"),
    CHIEF_DELIVERY_OFFICER(12, "Chief Delivery Officer"),
    DEPUTY_CEO(13, "Deputy CEO"),
    CEO(14, "CEO");

    @EnumValue
    private final int id;
    private final String name;
}
