package com.learning.emsmybatisliquibase.controller;

import com.learning.emsmybatisliquibase.entity.enums.OtpAuthType;
import com.learning.emsmybatisliquibase.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/sendOtp")
    public ResponseEntity<HttpStatus> sendOtp(@RequestParam("email") String email,
                                              @RequestParam("type") OtpAuthType type) {
        otpService.sendOtp(email, type);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/verifyOtp/{employeeUuid}")
    public ResponseEntity<HttpStatus> verifyOtp(@PathVariable UUID employeeUuid,
                                             @RequestParam("otp") String otp,
                                             @RequestParam("type") OtpAuthType type) {
        otpService.verifyOtp(employeeUuid, otp, type);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}