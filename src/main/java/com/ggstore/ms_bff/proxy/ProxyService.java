package com.ggstore.ms_bff.proxy;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Enumeration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProxyService {

    private final RestTemplate restTemplate;

    /**
     * Reenvía la petición HTTP entrante hacia targetBaseUrl, conservando método,
     * path, query string, headers (excepto Host) y body. Si usuarioId no es null,
     * agrega el header X-Usuario-Id para que el microservicio destino sepa quién
     * está haciendo la petición.
     */
    public ResponseEntity<byte[]> forward(HttpServletRequest request, String targetBaseUrl, UUID usuarioId) {
        String path = request.getRequestURI();
        String query = request.getQueryString();

        String url = targetBaseUrl + path + (query != null ? "?" + query : "");

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (name.equalsIgnoreCase("host") || name.equalsIgnoreCase("content-length")) continue;
            headers.add(name, request.getHeader(name));
        }
        if (usuarioId != null) {
            headers.set("X-Usuario-Id", usuarioId.toString());
        }

        byte[] body;
        try {
            body = request.getInputStream().readAllBytes();
        } catch (IOException e) {
            body = new byte[0];
        }

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        org.springframework.http.HttpEntity<byte[]> entity = new org.springframework.http.HttpEntity<>(
                body.length > 0 ? body : null, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    UriComponentsBuilder.fromUriString(url).build(true).toUri(),
                    method, entity, byte[].class);

            // Filtramos headers "hop-by-hop" que ya no son válidos para la respuesta
            // que estamos reconstruyendo (evita respuestas HTTP inconsistentes,
            // sobre todo en respuestas sin body como 204 No Content).
            HttpHeaders responseHeaders = new HttpHeaders();
            response.getHeaders().forEach((name, values) -> {
                if (name.equalsIgnoreCase("transfer-encoding")
                        || name.equalsIgnoreCase("content-length")
                        || name.equalsIgnoreCase("connection")) {
                    return;
                }
                responseHeaders.addAll(name, values);
            });

            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(response.getBody());
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders() != null ? e.getResponseHeaders() : new HttpHeaders())
                    .body(e.getResponseBodyAsByteArray());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(("{\"error\":\"No se pudo contactar el servicio destino: " + e.getMessage() + "\"}")
                            .getBytes());
        }
    }
}