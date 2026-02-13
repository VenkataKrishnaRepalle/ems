package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RoleType {
    ADMIN(1),
    EMPLOYEE(2),
    MANAGER(3),
    HR(4);

    @EnumValue
    private final int id;
}
