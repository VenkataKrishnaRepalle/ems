package com.learning.emsmybatisliquibase.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learning.emsmybatisliquibase.dto.EmployeeResponseDto;
import com.learning.emsmybatisliquibase.dto.LoginDto;
import com.learning.emsmybatisliquibase.dto.SuccessResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;

public interface AuthService {
    EmployeeResponseDto login(LoginDto loginDto, HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException;

    EmployeeResponseDto keycloakLogin(Jwt jwt, HttpServletRequest request, HttpServletResponse response);

    SuccessResponseDto verifyEmail(String email);

    EmployeeResponseDto refreshToken(String refreshToken, HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);
}
