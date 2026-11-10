package utn.frc.backend.mediturnos.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import utn.frc.backend.mediturnos.common.filter.CorrelationIdConstants;

import java.util.UUID;

/**
 * Filtro reactivo WebFilter de orden máximo (HIGHEST_PRECEDENCE) para trazabilidad distribuida.
 * Ejecuta antes del perímetro de seguridad, garantizando que tanto peticiones enrutadas
 * como respuestas de error 401/403 incluyan siempre la cabecera X-Correlation-Id.
 */
@Slf4j
@Component
public class ReactiveCorrelationIdFilter implements WebFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CorrelationIdConstants.CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        // Establecer en cabecera de respuesta inmediata y en el callback beforeCommit
        exchange.getResponse().getHeaders().set(CorrelationIdConstants.CORRELATION_ID_HEADER, finalCorrelationId);
        exchange.getResponse().beforeCommit(() -> Mono.fromRunnable(() -> {
            exchange.getResponse().getHeaders().set(CorrelationIdConstants.CORRELATION_ID_HEADER, finalCorrelationId);
        }));

        // Mutar la petición para propagar el X-Correlation-Id a los microservicios aguas abajo
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CorrelationIdConstants.CORRELATION_ID_HEADER, finalCorrelationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        long startTime = System.currentTimeMillis();
        log.info("[{}] Inicia {} {}", finalCorrelationId, request.getMethod(), request.getURI().getPath());

        return chain.filter(mutatedExchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = mutatedExchange.getResponse().getStatusCode() != null
                            ? mutatedExchange.getResponse().getStatusCode().value()
                            : 500;
                    log.info("[{}] Finaliza {} {} -> HTTP {} ({} ms)",
                            finalCorrelationId, request.getMethod(), request.getURI().getPath(), statusCode, duration);
                });
    }
}
