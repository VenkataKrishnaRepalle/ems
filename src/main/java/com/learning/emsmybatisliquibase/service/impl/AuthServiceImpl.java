package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.EmployeeDao;
import com.learning.emsmybatisliquibase.dao.EmployeeSessionDao;
import com.learning.emsmybatisliquibase.dao.PasswordDao;
import com.learning.emsmybatisliquibase.dto.EmployeeResponseDto;
import com.learning.emsmybatisliquibase.dto.LoginDto;
import com.learning.emsmybatisliquibase.dto.SuccessResponseDto;
import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.*;
import com.learning.emsmybatisliquibase.entity.enums.PasswordStatus;
import com.learning.emsmybatisliquibase.entity.enums.ProfileStatus;
import com.learning.emsmybatisliquibase.entity.enums.RoleType;
import com.learning.emsmybatisliquibase.exception.FoundException;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.security.JwtTokenProvider;
import com.learning.emsmybatisliquibase.service.EmployeeService;
import com.learning.emsmybatisliquibase.service.PasswordService;
import com.learning.emsmybatisliquibase.service.EmployeeRoleService;
import com.learning.emsmybatisliquibase.service.ProfileService;
import com.learning.emsmybatisliquibase.service.AuthService;
import com.learning.emsmybatisliquibase.utils.UtilityService;
import eu.bitwalker.useragentutils.UserAgent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeErrorCodes.PASSWORD_NOT_MATCHED;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final EmployeeService employeeService;

    private final PasswordDao passwordDao;

    private final PasswordService passwordService;

    private final EmployeeRoleService employeeRoleService;

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider jwtTokenProvider;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final ProfileService profileService;

    private final EmployeeSessionDao employeeSessionDao;

    private final WebClient webClient;

    private final EmployeeDao employeeDao;

    @Value("${maximum.login.count}")
    private Integer maxLoginCount;

    @Value("${api.location.key}")
    String key;

    @Value("${app.jwt-expiration-milliseconds}")
    private Integer jwtExpiryTime;

    @Value("${app.refresh-token-expiration-milliseconds}")
    private Integer refreshTokenExpiryTime;

    @Override
    @Transactional
    public EmployeeResponseDto login(LoginDto loginDto, HttpServletRequest request, HttpServletResponse response) {
        var employee = employeeService.findByEmail(loginDto.getEmail())
                .or(() -> employeeService.findByUsername(loginDto.getEmail()))
                .orElseThrow(() -> new InvalidInputException("INVALID_INPUT", "Invalid email or username"));
        var profile = profileService.getByEmployeeUuid(employee.getUuid());
        if (profile.getProfileStatus() == ProfileStatus.PENDING) {
            throw new InvalidInputException("ACCOUNT_NOT_ACTIVATED", "Account not activated, Please set new password");
        } else if (profile.getProfileStatus() == ProfileStatus.INACTIVE) {
            throw new InvalidInputException("NOT_AUTHORIZED_USER", "You're not eligible to access this application");
        }

        var passwords = passwordDao.getByEmployeeUuidAndStatus(employee.getUuid(),
                PasswordStatus.ACTIVE);
        if (passwords.size() != 1) {
            throw new InvalidInputException("ACCOUNT_LOCKED", "Account Locked, Please reset password");
        }
        var password = passwords.get(0);

        if (!passwordEncoder.matches(loginDto.getPassword(), password.getPassword())) {
            password.setNoOfIncorrectEntries(password.getNoOfIncorrectEntries() + 1);
            if (password.getNoOfIncorrectEntries() == 3) {
                password.setStatus(PasswordStatus.LOCKED);
            }
            passwordService.update(password);

            if (password.getNoOfIncorrectEntries() >= 3) {
                throw new InvalidInputException("ACCOUNT_LOCKED", "Your account locked, please reset your password");
            }
            throw new InvalidInputException(PASSWORD_NOT_MATCHED.code(), "Entered Password in Incorrect");
        }

        var sessions = employeeSessionDao.getByEmployeeUuid(employee.getUuid());
        if (sessions.size() >= maxLoginCount) {
            throw new FoundException("MAX_LOGIN_ATTEMPT_REACHED", "Max login attempts of " + maxLoginCount +
                    " reached to your account");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        String.valueOf(employee.getUuid()),
                        loginDto.getPassword()
                ));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        addAuthCookie(response, "token", token, jwtExpiryTime);
        addAuthCookie(response, "refreshToken", refreshToken, refreshTokenExpiryTime);

        if (null != loginDto.getRequestQuery()) {
            saveSession(request, employee, loginDto.getRequestQuery(), token);
        }

        var employeeResponse = employeeDao.getMe(employee.getUuid());
        employeeResponse.setRoles(getRoles(employee.getUuid()));
        return employeeResponse;
    }

    private List<String> getRoles(UUID employeeUuid) {
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

    private void saveSession(HttpServletRequest request, Employee employee, RequestQuery requestQuery, String token) {
        var userAgentString = request.getHeader("User-Agent");
        var userAgent = UserAgent.parseUserAgentString(userAgentString);
        var browser = userAgent.getBrowser();
        var os = userAgent.getOperatingSystem();
        var geoLocations = UtilityService.getLocationInfo(requestQuery);
        var longitude = geoLocations.get("longitude");
        var latitude = geoLocations.get("latitude");
        var location = getLocation(longitude, latitude);
        var platform = UtilityService.getPlatform(requestQuery);
        if (location == null || location.getProperties().isEmpty()) {
            return;
        }
        EmployeeSession session = EmployeeSession.builder()
                .uuid(UUID.randomUUID())
                .employeeUuid(employee.getUuid())
                .token(token)
                .longitude(longitude)
                .latitude(latitude)
                .browserName(browser.getName())
                .platform(platform)
                .osName(os.getName())
                .isActive(true)
                .location(UtilityService.extractAddressInfo(location))
                .loginTime(Instant.now())
                .build();
        try {
            if (0 == employeeSessionDao.insert(session)) {
                throw new IntegrityException("EMPLOYEE_SESSION_NOT_INSERTED",
                        "Employee Session not created for employee : " + employee.getUuid());
            }
        } catch (DataIntegrityViolationException exception) {
            throw new IntegrityException("EMPLOYEE_SESSION_NOT_INSERTED", exception.getCause().getMessage());
        }
    }

    @Override
    public SuccessResponseDto verifyEmail(String email) {
        var employee = employeeService.getByEmail(email);
        return SuccessResponseDto.builder()
                .data(employee == null ? null : employee.getUuid().toString())
                .success(employee != null)
                .build();
    }

    @Override
    public Map<String, Boolean> validateToken(String token, String refreshToken, HttpServletRequest request) {
        token = extractToken(token);
        refreshToken = extractToken(refreshToken);
        var isValidToken = token != null && jwtTokenProvider.validateToken(token);
        var isValidRefreshToken = refreshToken != null && jwtTokenProvider.validateRefreshToken(refreshToken);
        return Map.of("tokenActive", isValidToken, "refreshTokenActive", isValidRefreshToken);
    }

    private String extractToken(String token) {
        if (null == token) {
            return null;
        }
        if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    @Override
    public EmployeeResponseDto refreshToken(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new InvalidInputException("TOKEN_NOT_PROVIDED", "Token not provided");
        }
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }
        boolean isValid = jwtTokenProvider.validateRefreshToken(refreshToken);
        if (isValid) {
            var employeeId = UUID.fromString(jwtTokenProvider.getUsernameForRefreshToken(refreshToken));
            var employee = employeeService.getById(employeeId);
            var passwords = passwordDao.getByEmployeeUuidAndStatus(employeeId, PasswordStatus.ACTIVE);
            if (passwords.size() != 1) {
                throw new InvalidInputException("ACCOUNT_LOCKED", "Account Locked, Please reset password");
            }
            String token = jwtTokenProvider.generateToken(
                    new UsernamePasswordAuthenticationToken(
                            String.valueOf(employee.getUuid()),
                            passwords.get(0).getPassword()));

            addAuthCookie(response, "token", token, jwtExpiryTime);
            var employeeResponse = employeeDao.getMe(employeeId);
            employeeResponse.setRoles(getRoles(employeeId));

            return employeeResponse;
        } else {
            throw new InvalidInputException("INVALID_REFRESH_TOKEN", "Invalid refresh token");
        }
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        removeAuthCookie(response, "token");
        removeAuthCookie(response, "refreshToken");
    }

    private void removeAuthCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public boolean isCurrentUser(final UUID userId) {
        if (userId == null) {
            return false;
        }
        var currentUserId = getCurrentUserId();
        var employeeProfile = profileService.getByEmployeeUuid(userId);
        if (employeeProfile == null || !employeeProfile.getProfileStatus().equals(ProfileStatus.ACTIVE)) {
            return false;
        }
        return userId.equals(currentUserId);
    }

    private UUID getCurrentUserId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    public boolean isEmployeeManager(final UUID userId) {
        var currentUserId = getCurrentUserId();
        var employee = employeeService.getById(userId);
        return employee.getManagerUuid().equals(currentUserId);
    }

    @CircuitBreaker(name = "locationService", fallbackMethod = "getLocationFallback")
    private RequestQuery getLocation(String longitude, String latitude) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("")
                        .queryParam("q", latitude + "+" + longitude)
                        .queryParam("key", key)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    log.error("Error fetching Location: {}", clientResponse.statusCode());
                    return clientResponse.createException().flatMap(Mono::error);
                })
                .bodyToMono(RequestQuery.class)
                .block();
    }

    private RequestQuery getLocationFallback(Throwable throwable) {
        log.error("Location service is down: {}", throwable.getMessage());
        return new RequestQuery();
    }

}
