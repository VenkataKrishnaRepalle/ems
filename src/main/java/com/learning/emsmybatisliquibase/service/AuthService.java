package com.learning.emsmybatisliquibase.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learning.emsmybatisliquibase.dto.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.UUID;

public interface AuthService {
    JwtAuthResponseDto login(LoginDto loginDto, HttpServletRequest request) throws JsonProcessingException;

    SuccessResponseDto verifyEmail(String email);

    Map<String, Boolean> validateToken(UUID employeeId, String token);

    JwtAuthResponseDto refreshToken(String refreshToken, HttpServletRequest request);
}
