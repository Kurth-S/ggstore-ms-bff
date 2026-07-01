package com.ggstore.ms_bff.proxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyServiceTest {

    @Mock private RestTemplate restTemplate;
    @InjectMocks private ProxyService proxyService;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/juegos");
        request.setQueryString("titulo=zelda");
        request.addHeader("Authorization", "Bearer token-cliente");
    }

    @Test
    void forward_exitoso_reenviaYRetornaRespuestaDelServicioDestino() {
        byte[] cuerpoRespuesta = "{\"titulo\":\"Zelda\"}".getBytes(StandardCharsets.UTF_8);
        ResponseEntity<byte[]> respuestaMock = ResponseEntity.ok(cuerpoRespuesta);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(respuestaMock);

        ResponseEntity<byte[]> result = proxyService.forward(request, "http://localhost:8081", null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(cuerpoRespuesta);
    }

    @Test
    void forward_conUsuarioId_agregaHeaderXUsuarioId() {
        UUID usuarioId = UUID.randomUUID();
        ResponseEntity<byte[]> respuestaMock = ResponseEntity.ok(new byte[0]);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), entityCaptor.capture(), eq(byte[].class)))
                .thenReturn(respuestaMock);

        proxyService.forward(request, "http://localhost:8081", usuarioId);

        HttpHeaders headersEnviados = entityCaptor.getValue().getHeaders();
        assertThat(headersEnviados.getFirst("X-Usuario-Id")).isEqualTo(usuarioId.toString());
    }

    @Test
    void forward_construyeUrlConPathYQueryString() {
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        when(restTemplate.exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(ResponseEntity.ok(new byte[0]));

        proxyService.forward(request, "http://localhost:8081", null);

        assertThat(uriCaptor.getValue().toString()).isEqualTo("http://localhost:8081/juegos?titulo=zelda");
    }

    @Test
    void forward_siServicioDestinoRespondeConError_propagaElMismoStatus() {
        HttpClientErrorException excepcion = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY,
                "{\"error\":\"no encontrado\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(byte[].class)))
                .thenThrow(excepcion);

        ResponseEntity<byte[]> result = proxyService.forward(request, "http://localhost:8081", null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void forward_siServicioDestinoNoResponde_retornaBadGateway() {
        when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(byte[].class)))
                .thenThrow(new RuntimeException("Connection refused"));

        ResponseEntity<byte[]> result = proxyService.forward(request, "http://localhost:8081", null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(new String(result.getBody(), StandardCharsets.UTF_8)).contains("Connection refused");
    }
}