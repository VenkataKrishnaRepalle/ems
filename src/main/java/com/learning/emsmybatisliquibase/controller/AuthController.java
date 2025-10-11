package com.learning.emsmybatisliquibase.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learning.emsmybatisliquibase.dto.*;
import com.learning.emsmybatisliquibase.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @Value("${app.jwt-expiration-milliseconds}")
    private Integer expiryTime;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponseDto> login(@RequestBody LoginDto loginDto,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) throws JsonProcessingException {
        var result = authService.login(loginDto, request);
        Cookie cookie = new Cookie("token", result.getAccessToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(expiryTime);
        response.addCookie(cookie);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<JwtAuthResponseDto> refreshToken(@RequestBody String refreshToken,
                                                           HttpServletRequest request) {
        return new ResponseEntity<>(authService.refreshToken(refreshToken, request), HttpStatus.OK);
    }

    @PostMapping("/validate-token")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestParam("employeeId") UUID employeeId,
                                                              @RequestHeader("authorization") String token) {
        log.info("Employee Controller:: validateToken is called");
        return new ResponseEntity<>(authService.validateToken(employeeId, token), HttpStatus.OK);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<SuccessResponseDto> verifyEmail(@RequestParam(name = "email") String email) {
        return new ResponseEntity<>(authService.verifyEmail(email), HttpStatus.OK);
    }
}
