package utn.frc.backend.mediturnos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Contrato uniforme de respuesta de error HTTP para todos los microservicios de MediTurnos API.
 * Incluye trazabilidad mediante correlationId y el código de regla de negocio asociada (RN-01 a RN-08).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String ruleCode;
    private String message;
    private String path;
    private String correlationId;
    private Map<String, String> validationErrors;
}
