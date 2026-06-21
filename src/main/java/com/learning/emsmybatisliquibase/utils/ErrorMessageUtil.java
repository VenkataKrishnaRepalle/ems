package com.learning.emsmybatisliquibase.utils;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class ErrorMessageUtil {
    private ErrorMessageUtil() {
        /* This utility class should not be instantiated */
    }


    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("error_messages");

    public static String getMessage(String key, Object... params) {
        String message = RESOURCE_BUNDLE.getString(key);
        return MessageFormat.format(message, params);
    }
}