package com.ggstore.ms_bff.security;

import com.ggstore.ms_bff.enums.RolUsuario;
import com.ggstore.ms_bff.model.Usuario;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserPrincipal principal;
    private static final String SECRET = "clave-secreta-de-pruebas-para-jwt-con-al-menos-32-bytes-de-largo";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);

        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNombre("Matías");
        usuario.setEmail("matias@test.com");
        usuario.setPassword("hash");
        usuario.setRol(RolUsuario.USUARIO);

        principal = new UserPrincipal(usuario);
    }

    @Test
    void generateToken_yExtractEmail_devuelveElEmailCorrecto() {
        String token = jwtService.generateToken(principal);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("matias@test.com");
    }

    @Test
    void extractUserId_devuelveElIdCorrecto() {
        String token = jwtService.generateToken(principal);

        assertThat(jwtService.extractUserId(token)).isEqualTo(principal.getId().toString());
    }

    @Test
    void isTokenValid_conElMismoUsuario_retornaTrue() {
        String token = jwtService.generateToken(principal);

        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
    }

    @Test
    void isTokenValid_conUsuarioDiferente_retornaFalse() {
        String token = jwtService.generateToken(principal);

        Usuario otro = new Usuario();
        otro.setId(UUID.randomUUID());
        otro.setEmail("otro@test.com");
        otro.setPassword("hash");
        otro.setRol(RolUsuario.USUARIO);
        UserPrincipal otroPrincipal = new UserPrincipal(otro);

        assertThat(jwtService.isTokenValid(token, otroPrincipal)).isFalse();
    }

    @Test
    void token_expirado_lanzaExcepcionAlExtraerClaims() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -10000L);
        String tokenExpirado = jwtService.generateToken(principal);

        assertThatThrownBy(() -> jwtService.extractEmail(tokenExpirado))
                .isInstanceOf(ExpiredJwtException.class);
    }
}