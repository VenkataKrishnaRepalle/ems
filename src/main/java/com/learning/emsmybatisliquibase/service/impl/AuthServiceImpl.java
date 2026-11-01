package com.learning.emsmybatisliquibase.service.impl;

import com.learning.emsmybatisliquibase.dao.EmployeeDao;
import com.learning.emsmybatisliquibase.dao.PasswordDao;
import com.learning.emsmybatisliquibase.dto.EmployeeResponseDto;
import com.learning.emsmybatisliquibase.dto.LoginDto;
import com.learning.emsmybatisliquibase.dto.SuccessResponseDto;
import com.learning.emsmybatisliquibase.dto.pagination.RequestQuery;
import com.learning.emsmybatisliquibase.entity.Employee;
import com.learning.emsmybatisliquibase.entity.enums.PasswordStatus;
import com.learning.emsmybatisliquibase.entity.enums.ProfileStatus;
import com.learning.emsmybatisliquibase.entity.enums.RoleType;
import com.learning.emsmybatisliquibase.entity.enums.SessionStatus;
import com.learning.emsmybatisliquibase.exception.FoundException;
import com.learning.emsmybatisliquibase.exception.IntegrityException;
import com.learning.emsmybatisliquibase.exception.InvalidInputException;
import com.learning.emsmybatisliquibase.security.JwtTokenProvider;
import com.learning.emsmybatisliquibase.service.*;
import com.learning.emsmybatisliquibase.utils.Constants;
import com.learning.emsmybatisliquibase.utils.HashUtil;
import com.learning.emsmybatisliquibase.utils.UtilityService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import static com.learning.emsmybatisliquibase.exception.errorcodes.EmployeeErrorCodes.PASSWORD_NOT_MATCHED;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

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

    private final EmployeeSessionService employeeSessionService;

    private final WebClient webClient;

    private final EmployeeDao employeeDao;

    private final ObjectMapper objectMapper;

    @Value("${maximum.login.count}")
    private Integer maxLoginCount;

    @Value("${api.location.key}")
    String key;

    @Override
    @Transactional
    public EmployeeResponseDto login(LoginDto loginDto, HttpServletRequest request, HttpServletResponse response) {
        var employee = employeeService.findByEmail(loginDto.getEmail())
                .or(() -> employeeService.findByUsername(loginDto.getEmail()))
                .orElseThrow(() -> new InvalidInputException("INVALID_INPUT", "Invalid email or username"));
        var profile = profileService.getByEmployeeUuid(employee.getUuid());
        if (profile.getProfileStatus() == ProfileStatus.INACTIVE) {
            throw new InvalidInputException("NOT_AUTHORIZED_USER", "You're not eligible to access this application");
        }

        var passwords = passwordDao.getByEmployeeUuidAndStatus(employee.getUuid(), PasswordStatus.ACTIVE);
        if (passwords.size() != 1) {
            throw new InvalidInputException("ACCOUNT_LOCKED", "Account Locked, Please reset password");
        }
        var password = passwords.getFirst();

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

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(String.valueOf(employee.getUuid()), loginDto.getPassword()));

        return commonAuthenticationValidation(request, response, authentication, employee.getUuid(), loginDto.getPassword());
    }

    @Override
    public EmployeeResponseDto keycloakLogin(Jwt jwt, HttpServletRequest request, HttpServletResponse response) {
        String email = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("preferred_username");

        Employee employee = (StringUtils.hasText(email)
                ? employeeService.findByEmail(email)
                : employeeService.findByUsername(username))
                .orElseThrow(() -> {
                    log.error("Invalid email or preferred_username");
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED);
                });

        var password = passwordDao.getByEmployeeUuidAndStatus(employee.getUuid(), PasswordStatus.ACTIVE)
                .getFirst();
        if (password == null) {
            log.error("Invalid password / Password not set");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        List<String> roles = Collections.emptyList();

        if (realmAccess != null) {
            var rolesObject = realmAccess.get("roles");
            if (rolesObject instanceof List<?> roleList) {
                roles = roleList.stream()
                        .map(String.class::cast)
                        .toList();
            }
        }

        Set<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(String.valueOf(employee.getUuid()), null, authorities));

        return commonAuthenticationValidation(request, response, authentication, employee.getUuid(), null);
    }

    private EmployeeResponseDto commonAuthenticationValidation(HttpServletRequest request, HttpServletResponse response,
                                                               Authentication authentication, UUID employeeUuid, String password) {
        var sessionCount = employeeSessionService.count(new RequestQuery(
                Map.of("employee_uuid", employeeUuid,
                        "status", SessionStatus.ACTIVE,
                        "device_type", UtilityService.getDeviceDetails(request))));

        if (sessionCount >= maxLoginCount) {
            throw new FoundException("MAX_LOGIN_ATTEMPT_REACHED", "Max login attempts of " + maxLoginCount +
                    " reached to your account");
        }

        String sessionId = HashUtil.generateSessionId();
        String sessionIdHash = HashUtil.hash(sessionId);

        employeeSessionService.insert(request, sessionIdHash, employeeUuid);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = jwtTokenProvider.generateAccessToken(sessionId, authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(sessionId, authentication);

        addAuthCookies(response, sessionId, accessToken, refreshToken);

        var employeeResponse = employeeDao.getEmployee(employeeUuid);
        employeeResponse.setRoles(getRoles(employeeUuid));
        return employeeResponse;
    }

    private List<String> getRoles(UUID employeeUuid) {
        return employeeRoleService.getRolesByEmployeeUuid(employeeUuid)
                .stream()
                .map(RoleType::toString)
                .toList();
    }

    private void addAuthCookies(HttpServletResponse response, String sessionId,
                                String accessToken, String refreshToken) {
        if (sessionId != null) {
            addCookie(response, Constants.SESSION_ID, sessionId, Duration.ofHours(8), "/");
        }
        if (accessToken != null) {
            addCookie(response, Constants.ACCESS_TOKEN, accessToken, Duration.ofMinutes(15), "/");
        }
        if (refreshToken != null) {
            addCookie(response, Constants.REFRESH_TOKEN, refreshToken, Duration.ofHours(8), "/api/auth/refresh-token");
        }
    }

    private void addCookie(HttpServletResponse response, String name, String value,
                           Duration maxAge, String path) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")     // see note below about Vercel
                .path(path)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
    public EmployeeResponseDto refreshToken(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        var tokenFromCookie = UtilityService.getCookieValue(request, Constants.REFRESH_TOKEN);
        log.info("Token from Refresh Token Cookie: {}", tokenFromCookie);
        if (refreshToken == null && tokenFromCookie != null) {
            refreshToken = tokenFromCookie;
        }

        if (!StringUtils.hasText(refreshToken)) {
            throw new InvalidInputException("TOKEN_NOT_PROVIDED", "Token not provided");
        }
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }

        var claims = jwtTokenProvider.extractRefreshTokenAllClaims(refreshToken);
        log.info("Claims info from refresh token: {}", claims.toString());
        var sessionId = objectMapper.readValue(claims.get(Constants.SESSION_ID).toString(), String.class);
        var session = employeeSessionService.getBySessionId(HashUtil.hash(sessionId),
                List.of(SessionStatus.ACTIVE), UtilityService.getDeviceDetails(request));
        if (session == null) {
            throw new IntegrityException("INVALID_SESSION", "Invalid session id : " + sessionId);
        }
        if (claims.getExpiration().before(new Date())) {
            var employeeId = UUID.fromString(claims.getSubject());
            var employee = employeeService.getById(employeeId);
            var passwords = passwordDao.getByEmployeeUuidAndStatus(employeeId, PasswordStatus.ACTIVE);
            if (passwords.size() != 1) {
                throw new InvalidInputException("ACCOUNT_LOCKED", "Account Locked, Please reset password");
            }
            String accessToken = jwtTokenProvider.generateAccessToken(sessionId,
                    new UsernamePasswordAuthenticationToken(
                            String.valueOf(employee.getUuid()),
                            passwords.getFirst().getPassword()));

            addAuthCookies(response, null, accessToken, null);
            var employeeResponse = employeeDao.getEmployee(employeeId);
            employeeResponse.setRoles(getRoles(employeeId));

            return employeeResponse;
        } else {
            throw new InvalidInputException("INVALID_REFRESH_TOKEN", "Invalid refresh token");
        }
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        var sessionId = UtilityService.getCookieValue(request, Constants.SESSION_ID);
        employeeSessionService.deleteBySessionId(Objects.requireNonNull(HashUtil.hash(sessionId)));
        removeAuthCookie(response, Constants.SESSION_ID);
        removeAuthCookie(response, Constants.ACCESS_TOKEN);
        removeAuthCookie(response, Constants.REFRESH_TOKEN);
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