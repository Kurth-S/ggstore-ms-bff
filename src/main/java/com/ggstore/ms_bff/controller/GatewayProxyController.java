package com.ggstore.ms_bff.controller;

import com.ggstore.ms_bff.proxy.ProxyService;
import com.ggstore.ms_bff.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GatewayProxyController {

    private final ProxyService proxyService;

    @Value("${ms-catalogo.url}")
    private String msCatalogoUrl;

    @Value("${ms-pedidos.url}")
    private String msPedidosUrl;

    // Catálogo: juegos, categorías, reseñas -> ms-catalogo
    @RequestMapping(value = {"/juegos/**", "/categorias/**", "/resenas/**"})
    public ResponseEntity<byte[]> proxyCatalogo(
            HttpServletRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID usuarioId = principal != null ? principal.getId() : null;
        return proxyService.forward(request, msCatalogoUrl, usuarioId);
    }

    // Pedidos: carrito, checkout/historial, biblioteca, wishlist -> ms-pedidos
    @RequestMapping(value = {"/carrito/**", "/pedidos/**", "/biblioteca/**", "/wishlist/**"})
    public ResponseEntity<byte[]> proxyPedidos(
            HttpServletRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID usuarioId = principal != null ? principal.getId() : null;
        return proxyService.forward(request, msPedidosUrl, usuarioId);
    }
}
