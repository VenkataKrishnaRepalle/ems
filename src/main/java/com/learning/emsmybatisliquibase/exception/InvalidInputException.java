package com.learning.emsmybatisliquibase.exception;

import lombok.Getter;

@Getter
public class InvalidInputException extends RuntimeException {
    private final String errorCode;
    private final transient Object dynamicValue;

    public InvalidInputException(String errorCode, Object dynamicValue) {
        super(String.format("%s : %s", errorCode, dynamicValue));

        this.errorCode = errorCode;
        this.dynamicValue = dynamicValue;
    }
}
