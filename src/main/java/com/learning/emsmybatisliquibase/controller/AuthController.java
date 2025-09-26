package com.learning.emsmybatisliquibase.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learning.emsmybatisliquibase.dto.*;
import com.learning.emsmybatisliquibase.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponseDto> login(@RequestBody LoginDto loginDto,
                                                    HttpServletRequest request) throws JsonProcessingException {
        return new ResponseEntity<>(authService.login(loginDto, request), HttpStatus.OK);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<JwtAuthResponseDto> refreshToken(@RequestBody String refreshToken,
                                                           HttpServletRequest request) {
        return new ResponseEntity<>(authService.refreshToken(refreshToken, request), HttpStatus.OK);
    }

    @PostMapping("/validate-token")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestParam("employeeId") UUID employeeId,
                                                              @RequestHeader("authorization") String token) {
        return new ResponseEntity<>(authService.validateToken(employeeId, token), HttpStatus.OK);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<SuccessResponseDto> verifyEmail(@RequestParam(name = "email") String email) {
        return new ResponseEntity<>(authService.verifyEmail(email), HttpStatus.OK);
    }
}
