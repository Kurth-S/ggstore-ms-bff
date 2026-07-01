package com.ggstore.ms_bff.controller;

import com.ggstore.ms_bff.dto.AuthResponse;
import com.ggstore.ms_bff.dto.LoginRequest;
import com.ggstore.ms_bff.dto.RegisterRequest;
import com.ggstore.ms_bff.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @InjectMocks private AuthController authController;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Matías", "matias@test.com", "clave123");
        loginRequest = new LoginRequest("matias@test.com", "clave123");
        authResponse = new AuthResponse("token-jwt", "Matías", "matias@test.com", "USUARIO");
    }

    @Test
    void register_retorna201ConToken() {
        when(authService.register(registerRequest)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.register(registerRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().token()).isEqualTo("token-jwt");
        verify(authService).register(registerRequest);
    }

    @Test
    void login_retorna200ConToken() {
        when(authService.login(loginRequest)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.login(loginRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualTo("matias@test.com");
        verify(authService).login(loginRequest);
    }
}