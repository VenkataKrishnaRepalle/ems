package com.learning.emsmybatisliquibase.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeviceType {
    WEB(1),
    MOBILE(2),
    TABLET(3),
    UNKNOWN(4);

    private final int id;
}
