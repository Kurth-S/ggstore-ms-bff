# ms-bff

**Backend for Frontend** de GGStore. Es el único punto de entrada público de la API: maneja autenticación (registro/login con JWT), autoriza las peticiones, y actúa como proxy/gateway hacia `ms-catalogo` y `ms-pedidos`.

## Responsabilidad

- Registro y login de usuarios, emisión y validación de JWT.
- Autorización por rol (`USUARIO` / `ADMIN`) sobre las rutas.
- Proxy transparente: reenvía las peticiones del cliente hacia `ms-catalogo` o `ms-pedidos` según la ruta, agregando el header `X-Usuario-Id` a partir del JWT ya validado.
- Dashboard administrativo: combina datos propios (usuarios) con estadísticas de `ms-pedidos`.

Es el **único servicio con base de datos de usuarios** y el único que expone seguridad JWT — los demás microservicios confían en las peticiones que este BFF ya validó y reenvía.

## Stack técnico

| Componente | Detalle |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot (Web MVC) |
| Persistencia | Spring Data JPA + PostgreSQL (tabla de usuarios) |
| Seguridad | Spring Security + JWT (`jjwt`) |
| Cliente HTTP | `RestTemplate`, para el proxy hacia `ms-catalogo` / `ms-pedidos` |
| Validación | Spring Validation |
| Utilidades | Lombok |
| Tests | JUnit 5, Mockito, AssertJ |
| Cobertura | JaCoCo |

## Estructura del proyecto

```
src/main/java/com/ggstore/ms_bff/
├── controller/     # AuthController, AdminController, GatewayProxyController
├── service/        # AuthService
├── security/        # JwtService, JwtAuthenticationFilter, CustomUserDetailsService, UserPrincipal
├── proxy/          # ProxyService: arma y reenvía las peticiones HTTP
├── repository/     # UsuarioRepository
├── model/          # Entidad Usuario
├── dto/            # AuthResponse, LoginRequest, RegisterRequest, DashboardResponse
├── enums/          # RolUsuario
├── config/         # SecurityConfig, RestTemplateConfig
└── exception/      # GlobalExceptionHandler
```

## Cómo levantarlo local

### Requisitos
- JDK 17
- Maven (o el wrapper `mvnw` / `mvnw.cmd` incluido)
- Acceso a una base PostgreSQL (tabla de usuarios)
- `ms-catalogo` y `ms-pedidos` corriendo, para que el proxy funcione

### Variables de entorno / configuración

```properties
spring.application.name=ms-bff

spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=none
server.port=8080

jwt.secret=${JWT_SECRET}
jwt.expiration-ms=86400000

ms-catalogo.url=http://localhost:8081
ms-pedidos.url=http://localhost:8082
```

> ⚠️ El `application.properties` del repo tiene actualmente la credencial de la base **y** el `jwt.secret` hardcodeados en texto plano. Como este es el servicio que maneja autenticación, es el más urgente para migrar a variables de entorno / secrets — si el `jwt.secret` se filtra, cualquiera puede firmar tokens válidos.

### Levantar el servicio

```bash
./mvnw spring-boot:run
```

El servicio queda disponible en `http://localhost:8080`. Necesita `ms-catalogo` (`:8081`) y `ms-pedidos` (`:8082`) corriendo para que las rutas de proxy respondan.

## Endpoints principales

### Auth — `/auth` (público)
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/register` | Crea una cuenta y devuelve un JWT |
| POST | `/auth/login` | Autentica y devuelve un JWT |

### Admin — `/admin` (rol `ADMIN`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/admin/dashboard` | Combina cantidad de usuarios (propio) + estadísticas de ventas (`ms-pedidos`) |

### Proxy hacia otros servicios
Rutas reenviadas tal cual, agregando `X-Usuario-Id` cuando hay un usuario autenticado:

| Prefijo | Destino |
|---|---|
| `/juegos/**`, `/categorias/**`, `/resenas/**` | `ms-catalogo` |
| `/carrito/**`, `/pedidos/**`, `/biblioteca/**`, `/wishlist/**` | `ms-pedidos` |

### Reglas de autorización (`SecurityConfig`)
- `/auth/**` y `/error`: público.
- `GET /juegos/**`, `/categorias/**`, `/resenas/**`: público (navegar el catálogo sin login).
- `/admin/**`: requiere rol `ADMIN`.
- Cualquier otra ruta: requiere JWT válido.

## Autenticación

1. `POST /auth/login` devuelve un JWT.
2. El cliente lo manda en cada request como `Authorization: Bearer <token>`.
3. `JwtAuthenticationFilter` valida el token y carga el `UserPrincipal` en el contexto de seguridad.
4. `GatewayProxyController` toma el ID del usuario autenticado y lo agrega como header `X-Usuario-Id` al reenviar la petición a `ms-catalogo` o `ms-pedidos`.

## Tests y cobertura

```bash
./mvnw test
```

Cobertura por paquete (Mockito puro, sin levantar contexto de Spring):
- `service`, `config`, `dto`, `enums`: 100%
- `security`: 94%
- `controller`: 86%
- `proxy`: 81%
- `exception`: bajo — solo son excepciones custom sin lógica, no se prioriza

Reporte de cobertura:

```
target/site/jacoco/index.html
```

**Cobertura total actual: ~86% instructions / 55% branches.**

## Servicios relacionados

Este es el **punto de entrada** de todo el sistema — el cliente (frontend) solo habla con `ms-bff`, nunca directamente con `ms-catalogo` o `ms-pedidos`.

```
Cliente → ms-bff (auth + proxy) → ms-catalogo (catálogo)
                                 → ms-pedidos (carrito/checkout/pedidos)
```
