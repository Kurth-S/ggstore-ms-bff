package com.ggstore.ms_bff.controller;

import com.ggstore.ms_bff.dto.DashboardResponse;
import com.ggstore.ms_bff.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private AdminController adminController;

    private static final String MS_PEDIDOS_URL = "http://localhost:8082";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminController, "msPedidosUrl", MS_PEDIDOS_URL);
    }

    @Test
    void getDashboard_conStatsExitosos_retornaDashboardCompleto() {
        when(usuarioRepository.count()).thenReturn(25L);

        Map<String, Object> juegoVendido = Map.of(
                "juegoId", "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "titulo", "Cyberpunk 2077",
                "cantidadVendida", 12);

        Map<String, Object> ventaMes = Map.of(
                "mes", 6, "anio", 2026, "totalPedidos", 8, "ingresos", "1200000");

        Map<String, Object> stats = Map.of(
                "totalPedidos", 40,
                "ingresosTotales", "5000000",
                "juegosMasVendidos", List.of(juegoVendido),
                "ventasPorMes", List.of(ventaMes));

        when(restTemplate.exchange(eq(MS_PEDIDOS_URL + "/admin/stats"), eq(HttpMethod.GET), isNull(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(stats));

        ResponseEntity<DashboardResponse> response = adminController.getDashboard();

        DashboardResponse body = response.getBody();
        assertThat(body.getTotalUsuarios()).isEqualTo(25L);
        assertThat(body.getTotalPedidos()).isEqualTo(40L);
        assertThat(body.getIngresosTotales()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(body.getJuegosMasVendidos()).hasSize(1);
        assertThat(body.getJuegosMasVendidos().get(0).getTitulo()).isEqualTo("Cyberpunk 2077");
        assertThat(body.getVentasPorMes()).hasSize(1);
        assertThat(body.getVentasPorMes().get(0).getMes()).isEqualTo(6);
    }

    @Test
    void getDashboard_siMsPedidosNoResponde_retornaValoresPorDefecto() {
        when(usuarioRepository.count()).thenReturn(25L);
        when(restTemplate.exchange(eq(MS_PEDIDOS_URL + "/admin/stats"), eq(HttpMethod.GET), isNull(), eq(Map.class)))
                .thenThrow(new RestClientException("No se pudo conectar"));

        ResponseEntity<DashboardResponse> response = adminController.getDashboard();

        DashboardResponse body = response.getBody();
        assertThat(body.getTotalUsuarios()).isEqualTo(25L);
        assertThat(body.getTotalPedidos()).isEqualTo(0L);
        assertThat(body.getIngresosTotales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(body.getJuegosMasVendidos()).isEmpty();
        assertThat(body.getVentasPorMes()).isEmpty();
    }
}
