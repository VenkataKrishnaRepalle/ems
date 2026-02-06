package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dto.EmployeeResponseDto;
import com.learning.emsmybatisliquibase.dao.EmployeeDao;
import com.learning.emsmybatisliquibase.entity.enums.ProfileStatus;
import com.learning.emsmybatisliquibase.entity.enums.RoleType;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.security.JwtTokenProvider;
import com.learning.emsmybatisliquibase.service.EmployeeRoleService;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import com.learning.emsmybatisliquibase.service.KeycloakLoginService;
import com.learning.emsmybatisliquibase.service.ProfileService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.keycloak", name = "enabled", havingValue = "true")
public class KeycloakLoginServiceImpl implements KeycloakLoginService {

    private final EmployeeService employeeService;
    private final EmployeeDao employeeDao;
    private final ProfileService profileService;
    private final EmployeeRoleService employeeRoleService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtDecoder jwtDecoder;

    @Value("${app.jwt-expiration-milliseconds}")
    private Integer jwtExpiryTime;

    @Value("${app.refresh-token-expiration-milliseconds}")
    private Integer refreshTokenExpiryTime;

    @Override
    public EmployeeResponseDto login(String token, HttpServletResponse response) {
        Jwt jwt = jwtDecoder.decode(token);
        String email = jwt.getClaimAsString("email");
        String preferredUsername = jwt.getClaimAsString("preferred_username");

        if (!StringUtils.hasText(email) && !StringUtils.hasText(preferredUsername)) {
            throw new InvalidInputException("INVALID_INPUT", "Keycloak token missing username/email claims");
        }

        var employeeOptional = StringUtils.hasText(email)
                ? employeeService.findByEmail(email)
                : employeeService.findByUsername(preferredUsername);

        var employee = employeeOptional.orElseThrow(() ->
                new InvalidInputException("INVALID_INPUT", "No employee mapped for this Keycloak user"));

        var profile = profileService.getByEmployeeUuid(employee.getUuid());
        if (profile.getProfileStatus() == ProfileStatus.PENDING) {
            throw new InvalidInputException("ACCOUNT_NOT_ACTIVATED", "Account not activated, Please set new password");
        } else if (profile.getProfileStatus() == ProfileStatus.INACTIVE) {
            throw new InvalidInputException("NOT_AUTHORIZED_USER", "You're not eligible to access this application");
        }

        var authentication = new UsernamePasswordAuthenticationToken(String.valueOf(employee.getUuid()), null);
        String newToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        addAuthCookie(response, "token", newToken, jwtExpiryTime);
        addAuthCookie(response, "refreshToken", refreshToken, refreshTokenExpiryTime);

        var employeeResponse = employeeDao.getEmployee(employee.getUuid());
        employeeResponse.setRoles(getRoles(employee.getUuid()));
        return employeeResponse;
    }

    private List<String> getRoles(java.util.UUID employeeUuid) {
        return employeeRoleService.getRolesByEmployeeUuid(employeeUuid)
                .stream()
                .map(RoleType::toString)
                .toList();
    }

    private void addAuthCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}
