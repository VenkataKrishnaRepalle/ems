package com.learning.emsmybatisliquibase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.learning.emsmybatisliquibase.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    MockMvc mockMvc;

    @Mock
    AuthService authService;

    @InjectMocks
    AuthController authController;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

//    @Test
//    void login() throws Exception {
//        LoginDto loginDto = LoginDto.builder()
//                .email("test@gmail.com")
//                .password("password")
//                .build();
//        when(authService.login(any(LoginDto.class))).thenReturn(any(JwtAuthResponseDto.class));
//
//        mockMvc.perform(post("/api/auth/login")
//                .content(objectMapper.writeValueAsString(loginDto))
//                .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk());
//    }

}