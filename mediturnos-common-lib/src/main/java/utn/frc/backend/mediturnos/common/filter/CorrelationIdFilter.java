package utn.frc.backend.mediturnos.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import static utn.frc.backend.mediturnos.common.filter.CorrelationIdConstants.CORRELATION_ID_HEADER;
import static utn.frc.backend.mediturnos.common.filter.CorrelationIdConstants.CORRELATION_ID_MDC_KEY;

/**
 * Filtro HTTP transversal que lee el header X-Correlation-Id proveniente del API Gateway
 * o de otro microservicio, o genera un nuevo UUID si no está presente.
 * Lo almacena en el MDC de SLF4J para los logs y lo devuelve en el header de respuesta.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        } else {
            correlationId = correlationId.trim();
        }

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
