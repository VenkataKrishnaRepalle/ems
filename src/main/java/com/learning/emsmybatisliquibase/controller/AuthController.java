package com.learning.emsmybatisliquibase.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learning.emsmybatisliquibase.dto.*;
import com.learning.emsmybatisliquibase.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<EmployeeResponseDto> login(@RequestBody LoginDto loginDto,
                                                    HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException {
        var result = authService.login(loginDto, request, response);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<EmployeeResponseDto> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        return new ResponseEntity<>(authService.refreshToken(refreshToken, request, response), HttpStatus.OK);
    }

    @PostMapping("/validate-token")
    public ResponseEntity<Map<String, Boolean>> validateToken(@CookieValue(name = "token", required = false) String token,
                                                              @CookieValue(name = "refreshToken", required = false) String refreshToken,
                                                              HttpServletRequest request) {
        log.info("Employee Controller:: validateToken is called");
        return new ResponseEntity<>(authService.validateToken(token, refreshToken, request), HttpStatus.OK);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<SuccessResponseDto> verifyEmail(@RequestParam(name = "email") String email) {
        return new ResponseEntity<>(authService.verifyEmail(email), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
