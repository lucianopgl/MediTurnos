package utn.frc.backend.mediturnos.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import utn.frc.backend.mediturnos.common.filter.CorrelationIdConstants;
import utn.frc.backend.mediturnos.gateway.config.KeycloakReactiveJwtConverter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class ApiGatewaySecurityAndRoutingTests {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Autowired
    private KeycloakReactiveJwtConverter keycloakReactiveJwtConverter;

    private static HttpServer mockUpstreamServer;
    private static final AtomicReference<String> lastReceivedCorrelationId = new AtomicReference<>();
    private static final AtomicReference<String> lastReceivedAuthHeader = new AtomicReference<>();

    @BeforeAll
    static void startMockUpstreamServer() throws IOException {
        mockUpstreamServer = HttpServer.create(new InetSocketAddress(0), 0);

        HttpHandler handler = new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                lastReceivedCorrelationId.set(exchange.getRequestHeaders().getFirst(CorrelationIdConstants.CORRELATION_ID_HEADER));
                lastReceivedAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));

                byte[] responseBytes;
                int statusCode;

                if (exchange.getRequestURI().getPath().startsWith("/api/catalogo/sedes")) {
                    statusCode = 200;
                    responseBytes = "[{\"id\":1,\"nombre\":\"Sede Central\",\"activa\":true}]".getBytes(StandardCharsets.UTF_8);
                } else if (exchange.getRequestURI().getPath().startsWith("/api/reservas/turnos")) {
                    statusCode = 201;
                    responseBytes = "{\"id\":10,\"estado\":\"RESERVADO\"}".getBytes(StandardCharsets.UTF_8);
                } else {
                    statusCode = 200;
                    responseBytes = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
                }

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        };

        mockUpstreamServer.createContext("/api/catalogo", handler);
        mockUpstreamServer.createContext("/api/reservas", handler);
        mockUpstreamServer.start();
    }

    @AfterAll
    static void stopMockUpstreamServer() {
        if (mockUpstreamServer != null) {
            mockUpstreamServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void configureUpstreamUris(DynamicPropertyRegistry registry) {
        registry.add("CATALOGO_SERVICE_URI", () -> "http://localhost:" + mockUpstreamServer.getAddress().getPort());
        registry.add("RESERVAS_SERVICE_URI", () -> "http://localhost:" + mockUpstreamServer.getAddress().getPort());
    }

    // =========================================================================
    // 1. CV-08: Verificación de Rechazos 401 Unauthorized (Sin Token)
    // =========================================================================

    @Test
    @DisplayName("CV-08 (401): Petición sin token a /api/catalogo/sedes recibe 401 y ErrorResponseDTO")
    void givenNoToken_whenGetSedes_thenReturns401WithUnifiedErrorDTO() {
        webTestClient.get()
                .uri("/api/catalogo/sedes")
                .header(CorrelationIdConstants.CORRELATION_ID_HEADER, "corr-test-401-sedes")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(CorrelationIdConstants.CORRELATION_ID_HEADER, "corr-test-401-sedes")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("Unauthorized")
                .jsonPath("$.ruleCode").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").value(msg -> assertThat(msg.toString()).contains("se requiere un token Bearer JWT válido"))
                .jsonPath("$.path").isEqualTo("/api/catalogo/sedes")
                .jsonPath("$.correlationId").isEqualTo("corr-test-401-sedes")
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    @DisplayName("CV-08 (401): Petición sin token a /api/reservas/turnos recibe 401 y autogenera X-Correlation-Id")
    void givenNoTokenWithoutHeader_whenGetTurnos_thenReturns401WithGeneratedCorrelationId() {
        webTestClient.get()
                .uri("/api/reservas/turnos")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().exists(CorrelationIdConstants.CORRELATION_ID_HEADER)
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("Unauthorized")
                .jsonPath("$.ruleCode").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.correlationId").isNotEmpty();
    }

    // =========================================================================
    // 2. CV-08: Verificación de Rechazos 403 Forbidden (Roles Insuficientes)
    // =========================================================================

    @Test
    @DisplayName("CV-08 (403): Paciente intentando crear una sede (POST /api/catalogo/sedes) recibe 403 Forbidden")
    void givenPacienteToken_whenPostSede_thenReturns403WithUnifiedErrorDTO() {
        webTestClient
                .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROL_PACIENTE")))
                .post()
                .uri("/api/catalogo/sedes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nombre\": \"Sede Intento Invalido\", \"direccion\": \"Av Colón 123\"}")
                .header(CorrelationIdConstants.CORRELATION_ID_HEADER, "corr-test-403-admin")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().valueEquals(CorrelationIdConstants.CORRELATION_ID_HEADER, "corr-test-403-admin")
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.error").isEqualTo("Forbidden")
                .jsonPath("$.ruleCode").isEqualTo("FORBIDDEN")
                .jsonPath("$.message").value(msg -> assertThat(msg.toString()).contains("privilegios insuficientes"))
                .jsonPath("$.path").isEqualTo("/api/catalogo/sedes")
                .jsonPath("$.correlationId").isEqualTo("corr-test-403-admin");
    }

    @Test
    @DisplayName("CV-08 (403): Paciente intentando consultar notificaciones de auditoría (GET /api/reservas/notificaciones) recibe 403 Forbidden")
    void givenPacienteToken_whenGetAuditoriaNotificaciones_thenReturns403() {
        webTestClient
                .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROL_PACIENTE")))
                .get()
                .uri("/api/reservas/notificaciones")
                .header(CorrelationIdConstants.CORRELATION_ID_HEADER, "corr-test-403-notif")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().valueEquals(CorrelationIdConstants.CORRELATION_ID_HEADER, "corr-test-403-notif")
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.ruleCode").isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("CV-08 (403): Profesional intentando solicitar turno (POST /api/reservas/turnos) recibe 403 Forbidden")
    void givenProfesionalToken_whenPostTurno_thenReturns403() {
        webTestClient
                .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROL_PROFESIONAL")))
                .post()
                .uri("/api/reservas/turnos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"servicioId\": 1, \"profesionalId\": 2, \"fecha\": \"2026-11-15\", \"horaInicio\": \"09:00\"}")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.ruleCode").isEqualTo("FORBIDDEN");
    }

    // =========================================================================
    // 3. Verificación de Endpoints Públicos (PermitAll)
    // =========================================================================

    @Test
    @DisplayName("Endpoint público /actuator/health permite acceso anónimo e incluye X-Correlation-Id")
    void givenAnonymous_whenGetActuatorHealth_thenReturns200() {
        webTestClient.get()
                .uri("/actuator/health")
                .header(CorrelationIdConstants.CORRELATION_ID_HEADER, "corr-health-test")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CorrelationIdConstants.CORRELATION_ID_HEADER, "corr-health-test");
    }

    // =========================================================================
    // 4. Verificación de Enrutamiento, Token Relay y Propagación de Correlation-ID
    // =========================================================================

    @Test
    @DisplayName("Enrutamiento a Catálogo: Usuario autenticado GET /api/catalogo/sedes enruta y propaga X-Correlation-Id y Token")
    void givenAuthenticatedUser_whenGetSedes_thenRoutesToCatalogoAndPropagatesHeaders() {
        String testCid = "corr-route-catalogo-123";

        org.springframework.security.oauth2.jwt.Jwt mockJwtToken = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("mock-token-relay-test")
                .header("alg", "none")
                .subject("test-user-uuid")
                .claim("preferred_username", "juan_paciente")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("ROL_PACIENTE")))
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(3600))
                .build();

        org.mockito.Mockito.when(reactiveJwtDecoder.decode("mock-token-relay-test"))
                .thenReturn(reactor.core.publisher.Mono.just(mockJwtToken));

        webTestClient
                .get()
                .uri("/api/catalogo/sedes")
                .header(CorrelationIdConstants.CORRELATION_ID_HEADER, testCid)
                .header("Authorization", "Bearer mock-token-relay-test")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CorrelationIdConstants.CORRELATION_ID_HEADER, testCid)
                .expectBody()
                .jsonPath("$[0].nombre").isEqualTo("Sede Central");

        assertThat(lastReceivedCorrelationId.get()).isEqualTo(testCid);
        assertThat(lastReceivedAuthHeader.get()).isEqualTo("Bearer mock-token-relay-test");
    }

    @Test
    @DisplayName("Enrutamiento a Reservas: Paciente autenticado POST /api/reservas/turnos enruta y retorna 201 Created")
    void givenPaciente_whenPostTurno_thenRoutesToReservas() {
        String testCid = "corr-route-reservas-456";

        webTestClient
                .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROL_PACIENTE")))
                .post()
                .uri("/api/reservas/turnos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"profesionalId\": 1, \"servicioId\": 1, \"fecha\": \"2026-11-20\", \"horaInicio\": \"10:00\"}")
                .header(CorrelationIdConstants.CORRELATION_ID_HEADER, testCid)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals(CorrelationIdConstants.CORRELATION_ID_HEADER, testCid)
                .expectBody()
                .jsonPath("$.id").isEqualTo(10)
                .jsonPath("$.estado").isEqualTo("RESERVADO");

        assertThat(lastReceivedCorrelationId.get()).isEqualTo(testCid);
    }
}
