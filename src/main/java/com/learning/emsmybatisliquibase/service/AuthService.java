package com.learning.emsmybatisliquibase.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learning.emsmybatisliquibase.dto.EmployeeResponseDto;
import com.learning.emsmybatisliquibase.dto.LoginDto;
import com.learning.emsmybatisliquibase.dto.SuccessResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface AuthService {
    EmployeeResponseDto login(LoginDto loginDto, HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException;

    SuccessResponseDto verifyEmail(String email);

    Map<String, Boolean> validateToken(String token, String refreshToken, HttpServletRequest request);

    EmployeeResponseDto refreshToken(String refreshToken, HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);
}
