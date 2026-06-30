package com.ggstore.ms_bff.service;

import com.ggstore.ms_bff.dto.AuthResponse;
import com.ggstore.ms_bff.dto.LoginRequest;
import com.ggstore.ms_bff.dto.RegisterRequest;
import com.ggstore.ms_bff.enums.RolUsuario;
import com.ggstore.ms_bff.model.Usuario;
import com.ggstore.ms_bff.repository.UsuarioRepository;
import com.ggstore.ms_bff.security.JwtService;
import com.ggstore.ms_bff.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRol(RolUsuario.USUARIO);

        usuarioRepository.save(usuario);

        UserPrincipal principal = new UserPrincipal(usuario);
        String token = jwtService.generateToken(principal);

        return new AuthResponse(token, usuario.getNombre(), usuario.getEmail(), usuario.getRol().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        UserPrincipal principal = new UserPrincipal(usuario);
        String token = jwtService.generateToken(principal);

        return new AuthResponse(token, usuario.getNombre(), usuario.getEmail(), usuario.getRol().name());
    }
}
