package utn.frc.backend.mediturnos.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import utn.frc.backend.mediturnos.common.dto.ErrorResponseDTO;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static utn.frc.backend.mediturnos.common.filter.CorrelationIdConstants.CORRELATION_ID_HEADER;
import static utn.frc.backend.mediturnos.common.filter.CorrelationIdConstants.CORRELATION_ID_MDC_KEY;

/**
 * Manejador centralizado de excepciones para todos los microservicios de MediTurnos API.
 * Traduce las excepciones de negocio (RN-01 a RN-08) y los errores de validación (@Valid)
 * a una estructura uniforme ErrorResponseDTO con el código HTTP exacto y el X-Correlation-Id.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MediTurnosBaseException.class)
    public ResponseEntity<ErrorResponseDTO> handleMediTurnosException(
            MediTurnosBaseException ex,
            HttpServletRequest request
    ) {
        String correlationId = resolveCorrelationId(request);
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.UNPROCESSABLE_ENTITY;

        log.warn("[{}] Regla {} rechazada en {} ({}): {}",
                correlationId, ex.getRuleCode(), request.getRequestURI(), status.value(), ex.getMessage());

        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .ruleCode(ex.getRuleCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String correlationId = resolveCorrelationId(request);
        Map<String, String> validationErrors = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("[{}] Error de validación de entrada en {}: {}",
                correlationId, request.getRequestURI(), validationErrors);

        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .ruleCode("VALIDATION_ERROR")
                .message("Error de validación en los campos de la solicitud")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        String correlationId = resolveCorrelationId(request);
        log.warn("[{}] Argumento inválido en {}: {}", correlationId, request.getRequestURI(), ex.getMessage());

        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .ruleCode("BAD_REQUEST")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        String correlationId = resolveCorrelationId(request);
        log.error("[{}] Error interno no esperado en {}: {}",
                correlationId, request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .ruleCode("INTERNAL_ERROR")
                .message("Ha ocurrido un error interno al procesar la solicitud")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader(CORRELATION_ID_HEADER);
        }
        return correlationId;
    }
}
