package com.ggstore.ms_bff.security;

import com.ggstore.ms_bff.enums.RolUsuario;
import com.ggstore.ms_bff.model.Usuario;
import com.ggstore.ms_bff.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private CustomUserDetailsService userDetailsService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNombre("Matías");
        usuario.setEmail("matias@test.com");
        usuario.setPassword("hash");
        usuario.setRol(RolUsuario.ADMIN);
    }

    @Test
    void loadUserByUsername_usuarioExiste_retornaUserPrincipal() {
        when(usuarioRepository.findByEmail("matias@test.com")).thenReturn(Optional.of(usuario));

        UserDetails result = userDetailsService.loadUserByUsername("matias@test.com");

        assertThat(result).isInstanceOf(UserPrincipal.class);
        assertThat(result.getUsername()).isEqualTo("matias@test.com");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_usuarioNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("noexiste@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("noexiste@test.com");
    }
}
