package com.ggstore.ms_bff.security;

import com.ggstore.ms_bff.enums.RolUsuario;
import com.ggstore.ms_bff.model.Usuario;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("matias@test.com");
        usuario.setPassword("hash");
        usuario.setRol(RolUsuario.USUARIO);
        principal = new UserPrincipal(usuario);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_sinHeaderAuthorization_continuaSinAutenticar() throws Exception {
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void doFilterInternal_conTokenValido_autenticaAlUsuario() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");

        when(jwtService.extractEmail("token-valido")).thenReturn("matias@test.com");
        when(userDetailsService.loadUserByUsername("matias@test.com")).thenReturn(principal);
        when(jwtService.isTokenValid("token-valido", principal)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_conTokenInvalido_noAutentica() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");

        when(jwtService.extractEmail("token-valido")).thenReturn("matias@test.com");
        when(userDetailsService.loadUserByUsername("matias@test.com")).thenReturn(principal);
        when(jwtService.isTokenValid("token-valido", principal)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_conTokenMalformado_capturaExcepcionYContinua() throws Exception {
        request.addHeader("Authorization", "Bearer token-corrupto");

        when(jwtService.extractEmail("token-corrupto")).thenThrow(new RuntimeException("JWT inválido"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}