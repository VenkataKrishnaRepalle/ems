package com.learning.emsmybatisliquibase.exception;

import com.learning.emsmybatisliquibase.exception.api.ErrorField;
import com.learning.emsmybatisliquibase.exception.api.ErrorMessage;
import com.learning.emsmybatisliquibase.exception.api.ErrorResponse;
import com.learning.emsmybatisliquibase.exception.api.FieldErrorListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestControllerAdvice
public class CustomErrorController {
    @ExceptionHandler(BindException.class)
    public ResponseEntity<FieldErrorListResponse> handleValidationException(BindException bindException) {
        BindingResult bindingResult = bindException.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();

        List<ErrorField> errors = new ArrayList<>();
        for (FieldError fieldError : fieldErrors) {
            String[] errorCodeAndMessage = Objects.requireNonNull(fieldError.getDefaultMessage()).split("-");
            String errorCode = errorCodeAndMessage[0];
            String errorMessage = errorCodeAndMessage[1];

            errors.add(ErrorField.builder()
                    .code(errorCode)
                    .message(MessageFormat.format(errorMessage, fieldError.getField()))
                    .fieldName(fieldError.getField())
                    .build());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(FieldErrorListResponse.builder().errors(errors).build());
    }

    @ExceptionHandler(FoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerAlreadyPresentExceptionError(FoundException exception) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .body(ErrorMessage.errorResponse(exception.getErrorCode(), exception.getDynamicValue()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFoundExceptionError(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorMessage.errorResponse(exception.getErrorCode(), exception.getDynamicValue()));
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleLoginExceptionError(InvalidInputException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorMessage.errorResponse(exception.getErrorCode(), exception.getDynamicValue()));
    }

    @ExceptionHandler(IntegrityException.class)
    public ResponseEntity<ErrorResponse> handleIntegrityExceptionError(IntegrityException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorMessage.errorResponse(exception.getErrorCode(), exception.getDynamicValue()));
    }
}