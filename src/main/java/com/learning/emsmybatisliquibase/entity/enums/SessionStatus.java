package com.learning.emsmybatisliquibase.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SessionStatus {
    ACTIVE(1),
    LOGGED_OUT(2),
    REVOKED(3),
    EXPIRED(4);

    private final int id;
}
