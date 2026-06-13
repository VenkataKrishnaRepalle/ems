package com.learning.emsmybatisliquibase.exception.api;


public class ErrorMessage {

    private ErrorMessage() {
    }

    public static ErrorResponse errorResponse(String error, Object dynamicValue) {
        return ErrorResponse.builder()
                .status(StatusType.ERROR)
                .error(Error.builder()
                        .code(error)
                        .message(dynamicValue)
                        .build())
                .build();
    }
}
