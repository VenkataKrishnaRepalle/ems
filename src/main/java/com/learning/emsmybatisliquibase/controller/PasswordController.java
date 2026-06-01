package com.learning.emsmybatisliquibase.controller;

import com.learning.emsmybatisliquibase.dto.ForgotPasswordDto;
import com.learning.emsmybatisliquibase.dto.ResetPasswordDto;
import com.learning.emsmybatisliquibase.dto.SuccessResponseDto;
import com.learning.emsmybatisliquibase.service.PasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordService passwordService;

    @PostMapping("/forgotPassword")
    public ResponseEntity<SuccessResponseDto> forgotPassword(@RequestBody @Valid ForgotPasswordDto forgotPasswordDto) {
        return new ResponseEntity<>(passwordService.forgotPassword(forgotPasswordDto), HttpStatus.OK);
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<SuccessResponseDto> resetPassword(@RequestBody @Valid ResetPasswordDto resetPasswordDto) {
        return new ResponseEntity<>(passwordService.resetPassword(resetPasswordDto), HttpStatus.OK);
    }
}
