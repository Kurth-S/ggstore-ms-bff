package com.ggstore.ms_bff.controller;

import com.ggstore.ms_bff.dto.DashboardResponse;
import com.ggstore.ms_bff.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final RestTemplate restTemplate;

    @Value("${ms-pedidos.url}")
    private String msPedidosUrl;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardResponse> getDashboard() {
        Long totalUsuarios = usuarioRepository.count();

        try {
            ResponseEntity<Map> statsResponse = restTemplate.exchange(
                    msPedidosUrl + "/admin/stats",
                    HttpMethod.GET,
                    null,
                    Map.class
            );

            Map<String, Object> stats = statsResponse.getBody();

            Long totalPedidos = stats != null ? ((Number) stats.get("totalPedidos")).longValue() : 0L;
            BigDecimal ingresosTotales = stats != null
                    ? new BigDecimal(stats.get("ingresosTotales").toString())
                    : BigDecimal.ZERO;

            List<DashboardResponse.JuegoVendidoDTO> juegosMasVendidos = new ArrayList<>();
            if (stats != null && stats.get("juegosMasVendidos") instanceof List<?> lista) {
                for (Object item : lista) {
                    if (item instanceof Map<?, ?> m) {
                        UUID juegoId = UUID.fromString(m.get("juegoId").toString());
                        String titulo = m.get("titulo").toString();
                        Long cantidad = ((Number) m.get("cantidadVendida")).longValue();
                        juegosMasVendidos.add(new DashboardResponse.JuegoVendidoDTO(juegoId, titulo, cantidad));
                    }
                }
            }

            List<DashboardResponse.VentasMesDTO> ventasPorMes = new ArrayList<>();
            if (stats != null && stats.get("ventasPorMes") instanceof List<?> lista) {
                for (Object item : lista) {
                    if (item instanceof Map<?, ?> m) {
                        Integer mes = ((Number) m.get("mes")).intValue();
                        Integer anio = ((Number) m.get("anio")).intValue();
                        Long totalMes = ((Number) m.get("totalPedidos")).longValue();
                        BigDecimal ingresos = new BigDecimal(m.get("ingresos").toString());
                        ventasPorMes.add(new DashboardResponse.VentasMesDTO(mes, anio, totalMes, ingresos));
                    }
                }
            }

            return ResponseEntity.ok(new DashboardResponse(
                    totalUsuarios, totalPedidos, ingresosTotales,
                    juegosMasVendidos, ventasPorMes));

        } catch (Exception e) {
            return ResponseEntity.ok(new DashboardResponse(
                    totalUsuarios, 0L, BigDecimal.ZERO,
                    new ArrayList<>(), new ArrayList<>()));
        }
    }
}