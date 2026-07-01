package com.ggstore.ms_bff.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private Long totalUsuarios;
    private Long totalPedidos;
    private BigDecimal ingresosTotales;
    private List<JuegoVendidoDTO> juegosMasVendidos;
    private List<VentasMesDTO> ventasPorMes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JuegoVendidoDTO {
        private UUID juegoId;
        private String titulo;
        private Long cantidadVendida;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VentasMesDTO {
        private Integer mes;
        private Integer anio;
        private Long totalPedidos;
        private BigDecimal ingresos;
    }
}