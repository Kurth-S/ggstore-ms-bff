package com.ggstore.ms_bff.dto;

public record AuthResponse(
        String token,
        String nombre,
        String email,
        String rol
) {}
