package com.ggstore.ms_bff.controller;

import com.ggstore.ms_bff.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UsuarioRepository usuarioRepository;

    @Value("${ms-pedidos.url}")
    private String msPedidosUrl;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        // Cuenta usuarios desde la base propia del BFF
        long totalUsuarios = usuarioRepository.count();

        // Pide las stats de pedidos a ms-pedidos
        RestClient restClient = RestClient.builder()
                .baseUrl(msPedidosUrl)
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> statsPedidos = restClient.get()
                .uri("/admin/dashboard")
                .retrieve()
                .body(Map.class);

        // Combina todo en una sola respuesta
        assert statsPedidos != null;
        statsPedidos.put("totalUsuarios", totalUsuarios);

        return ResponseEntity.ok(statsPedidos);
    }
}
