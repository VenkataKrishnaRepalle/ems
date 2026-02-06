package com.learning.emsmybatisliquibase.controller;

import com.learning.emsmybatisliquibase.dto.EmployeeResponseDto;
import com.learning.emsmybatisliquibase.service.KeycloakLoginService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/keycloak")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@ConditionalOnProperty(prefix = "app.keycloak", name = "enabled", havingValue = "true")
public class KeycloakLoginController {

    private final KeycloakLoginService keycloakLoginService;

    @PostMapping("/login-keycloak")
    public ResponseEntity<EmployeeResponseDto> loginKeycloak(@RequestBody String token,
                                                             HttpServletResponse response) {
        return new ResponseEntity<>(keycloakLoginService.login(token, response), HttpStatus.OK);
    }
}
