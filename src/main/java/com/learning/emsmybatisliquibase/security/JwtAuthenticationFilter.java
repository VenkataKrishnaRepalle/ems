package com.learning.emsmybatisliquibase.security;

import com.learning.emsmybatisliquibase.service.EmployeeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final ObjectProvider<JwtDecoder> keycloakJwtDecoderProvider;
    private final EmployeeService employeeService;


    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserDetailsService userDetailsService,
                                   ObjectProvider<JwtDecoder> keycloakJwtDecoderProvider,
                                   EmployeeService employeeService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.keycloakJwtDecoderProvider = keycloakJwtDecoderProvider;
        this.employeeService = employeeService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (request.getMethod().equals("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (var cookie : cookies) {
                if (cookie.getName().equals("token")) {
                    token = cookie.getValue();
                }
            }
        }
        if (token == null) {
            token = getTokenFromRequest(request);
        }

        if (StringUtils.hasText(token)) {
            if (isKeycloakToken(token)) {
                authenticateWithKeycloakJwtIfPossible(token, request);
            } else if (jwtTokenProvider.validateToken(token, request)) {
                String username = jwtTokenProvider.getUsername(token);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isKeycloakToken(String token) {
        String alg = getJwtHeaderValue(token);
        return StringUtils.hasText(alg) && alg.startsWith("RS");
    }

    private void authenticateWithKeycloakJwtIfPossible(String rawToken, HttpServletRequest request) {
        JwtDecoder jwtDecoder = keycloakJwtDecoderProvider.getIfAvailable();
        if (jwtDecoder == null) {
            request.setAttribute("not_authorized", true);
            return;
        }

        try {
            var jwt = jwtDecoder.decode(rawToken);
            String email = jwt.getClaimAsString("email");
            String preferredUsername = jwt.getClaimAsString("preferred_username");

            if (!StringUtils.hasText(email) && !StringUtils.hasText(preferredUsername)) {
                request.setAttribute("not_authorized", true);
                return;
            }

            var employeeOptional = StringUtils.hasText(email)
                    ? employeeService.findByEmail(email)
                    : employeeService.findByUsername(preferredUsername);

            var employee = employeeOptional.orElse(null);
            if (employee == null) {
                request.setAttribute("not_authorized", true);
                return;
            }

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            List<String> roles = List.of();
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                Object rolesObj = realmAccess.get("roles");

                if (!(rolesObj instanceof List<?> rawList)) {
                    throw new IllegalArgumentException("'roles' is not a list");
                }
                roles = rawList.stream()
                        .map(String.class::cast)
                        .toList();
            }

            Set<GrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toSet());

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    employee.getUuid(), null, authorities);

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        } catch (JwtException ex) {
            request.setAttribute("not_authorized", true);
        }
    }

    private String getJwtHeaderValue(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[0]);
            String json = new String(decoded, StandardCharsets.UTF_8);
            Map<String, Object> values = OBJECT_MAPPER.readValue(json, new TypeReference<>() {
            });
            Object value = values.get("alg");
            return value == null ? null : String.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
