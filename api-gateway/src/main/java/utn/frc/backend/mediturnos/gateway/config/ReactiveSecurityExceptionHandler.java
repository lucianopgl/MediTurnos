package utn.frc.backend.mediturnos.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import utn.frc.backend.mediturnos.common.dto.ErrorResponseDTO;
import utn.frc.backend.mediturnos.common.filter.CorrelationIdConstants;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Manejador reactivo unificado de excepciones de seguridad perimetral para Spring Cloud Gateway.
 * Traduce fallos de autenticación (401) y autorización (403) al contrato uniforme ErrorResponseDTO
 * de mediturnos-common-lib, garantizando la inclusión del header y campo X-Correlation-Id.
 */
@Slf4j
@Component
public class ReactiveSecurityExceptionHandler implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ReactiveSecurityExceptionHandler(ObjectMapper objectMapper) {
        ObjectMapper mapper = (objectMapper != null) ? objectMapper.copy() : new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        this.objectMapper = mapper;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = resolveCorrelationId(exchange);
        log.warn("[{}] Rechazo 401 Unauthorized en {}: {}", correlationId, path, ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .ruleCode("UNAUTHORIZED")
                .message("No autenticado: se requiere un token Bearer JWT válido")
                .path(path)
                .correlationId(correlationId)
                .build();

        return writeErrorResponse(exchange.getResponse(), HttpStatus.UNAUTHORIZED, error, correlationId);
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = resolveCorrelationId(exchange);
        log.warn("[{}] Rechazo 403 Forbidden en {}: {}", correlationId, path, denied.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .ruleCode("FORBIDDEN")
                .message("Acceso denegado: roles o privilegios insuficientes para esta operación")
                .path(path)
                .correlationId(correlationId)
                .build();

        return writeErrorResponse(exchange.getResponse(), HttpStatus.FORBIDDEN, error, correlationId);
    }

    private Mono<Void> writeErrorResponse(ServerHttpResponse response, HttpStatus status, ErrorResponseDTO error, String correlationId) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (correlationId != null && !correlationId.isBlank()) {
            response.getHeaders().set(CorrelationIdConstants.CORRELATION_ID_HEADER, correlationId);
        }

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(error).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = "{\"status\":500,\"message\":\"Error serializando respuesta de error\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private String resolveCorrelationId(ServerWebExchange exchange) {
        String cid = exchange.getRequest().getHeaders().getFirst(CorrelationIdConstants.CORRELATION_ID_HEADER);
        if (cid == null || cid.isBlank()) {
            cid = exchange.getResponse().getHeaders().getFirst(CorrelationIdConstants.CORRELATION_ID_HEADER);
        }
        return cid;
    }
}
