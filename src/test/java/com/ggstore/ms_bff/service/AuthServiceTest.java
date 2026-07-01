package com.ggstore.ms_bff.service;

import com.ggstore.ms_bff.dto.AuthResponse;
import com.ggstore.ms_bff.dto.LoginRequest;
import com.ggstore.ms_bff.dto.RegisterRequest;
import com.ggstore.ms_bff.enums.RolUsuario;
import com.ggstore.ms_bff.model.Usuario;
import com.ggstore.ms_bff.repository.UsuarioRepository;
import com.ggstore.ms_bff.security.JwtService;
import com.ggstore.ms_bff.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Matías", "matias@test.com", "clave123");
        loginRequest = new LoginRequest("matias@test.com", "clave123");

        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNombre("Matías");
        usuario.setEmail("matias@test.com");
        usuario.setPassword("hash-encriptado");
        usuario.setRol(RolUsuario.USUARIO);
    }

    @Test
    void register_creaUsuarioNuevo_retornaTokenYDatos() {
        when(usuarioRepository.existsByEmail("matias@test.com")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("hash-encriptado");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("token-jwt");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.token()).isEqualTo("token-jwt");
        assertThat(response.email()).isEqualTo("matias@test.com");
        assertThat(response.rol()).isEqualTo("USUARIO");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hash-encriptado");
        assertThat(captor.getValue().getRol()).isEqualTo(RolUsuario.USUARIO);
    }

    @Test
    void register_siEmailYaExiste_lanzaExcepcion() {
        when(usuarioRepository.existsByEmail("matias@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe una cuenta");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_credencialesValidas_retornaToken() {
        when(usuarioRepository.findByEmail("matias@test.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("token-jwt");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.token()).isEqualTo("token-jwt");
        assertThat(response.nombre()).isEqualTo("Matías");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_usuarioNoEncontradoTrasAutenticar_lanzaExcepcion() {
        when(usuarioRepository.findByEmail("matias@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciales inválidas");
    }

    @Test
    void login_credencialesInvalidas_propagaExcepcionDeSpringSecurity() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(usuarioRepository, never()).findByEmail(any());
    }
}