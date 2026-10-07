package com.learning.emsmybatisliquibase.exception.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Error {
    private String code;
    private Object message;
}
