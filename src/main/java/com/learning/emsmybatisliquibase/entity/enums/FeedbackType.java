package com.learning.emsmybatisliquibase.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum FeedbackType {
    DRAFT(1),
    SEND(2),
    REQUEST(3),
    RESPOND(4);

    @EnumValue
    @Getter
    private final int id;
}
