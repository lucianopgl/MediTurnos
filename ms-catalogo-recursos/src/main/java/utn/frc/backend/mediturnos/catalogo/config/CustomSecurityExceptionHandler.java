package utn.frc.backend.mediturnos.catalogo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import utn.frc.backend.mediturnos.common.dto.ErrorResponseDTO;
import utn.frc.backend.mediturnos.common.filter.CorrelationIdConstants;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Maneja los rechazos de autenticación (401) y autorización (403)
 * retornando la estructura estandarizada ErrorResponseDTO con X-Correlation-Id.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomSecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String correlationId = resolveCorrelationId(request);
        log.warn("[{}] Intento de acceso no autenticado en {}: {}",
                correlationId, request.getRequestURI(), authException.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .ruleCode("UNAUTHORIZED")
                .message("No autenticado: se requiere un token Bearer JWT válido")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        writeResponse(response, HttpStatus.UNAUTHORIZED, error, correlationId);
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String correlationId = resolveCorrelationId(request);
        log.warn("[{}] Acceso denegado por falta de rol en {}: {}",
                correlationId, request.getRequestURI(), accessDeniedException.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .ruleCode("FORBIDDEN")
                .message("Acceso denegado: roles o privilegios insuficientes para esta operación")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        writeResponse(response, HttpStatus.FORBIDDEN, error, correlationId);
    }

    private void writeResponse(HttpServletResponse response,
                               HttpStatus status,
                               ErrorResponseDTO error,
                               String correlationId) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        if (correlationId != null) {
            response.setHeader(CorrelationIdConstants.CORRELATION_ID_HEADER, correlationId);
        }
        objectMapper.writeValue(response.getWriter(), error);
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CorrelationIdConstants.CORRELATION_ID_HEADER);
        return (correlationId != null && !correlationId.isBlank()) ? correlationId : null;
    }
}
