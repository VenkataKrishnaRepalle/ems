package com.learning.emsmybatisliquibase.service;

import com.learning.emsmybatisliquibase.dto.EmployeeResponseDto;
import jakarta.servlet.http.HttpServletResponse;

public interface KeycloakLoginService {

    EmployeeResponseDto login(String token, HttpServletResponse response);
}
