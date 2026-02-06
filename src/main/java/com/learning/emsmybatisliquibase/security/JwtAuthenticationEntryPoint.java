package com.learning.emsmybatisliquibase.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String ERROR_CODE = "errorCode";

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Map<String, String> errorResponse = new LinkedHashMap<>();

        if (Boolean.TRUE.equals(request.getAttribute("tokenExpired"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            errorResponse.put(ERROR_CODE, "TOKEN_EXPIRED");
            errorResponse.put(ERROR_MESSAGE, "Token has expired");
        } else if (Boolean.TRUE.equals(request.getAttribute("invalidToken"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            errorResponse.put(ERROR_CODE, "INVALID_TOKEN_PROVIDED");
            errorResponse.put(ERROR_MESSAGE, "Invalid token");
        } else if (Boolean.TRUE.equals(request.getAttribute("not_authorized"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            errorResponse.put(ERROR_CODE, "NOT_AUTHORIZED");
            errorResponse.put(ERROR_MESSAGE, "You're not authorized to access this resource");
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            errorResponse.put(ERROR_CODE, "TOKEN_NOT_PROVIDED");
            errorResponse.put(ERROR_MESSAGE, "Missing Authorization header or token cookie");
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(jsonResponse);
    }
}
