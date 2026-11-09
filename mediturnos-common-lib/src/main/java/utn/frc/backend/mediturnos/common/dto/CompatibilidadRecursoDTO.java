package utn.frc.backend.mediturnos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contrato de recurso compartido entre ms-catalogo-recursos y ms-reservas (FUNC-05).
 * Representa la respuesta sustantiva de compatibilidad de catálogo en GET /api/catalogo/compatibilidad
 * una vez evaluadas RN-01 y RN-02, proveyendo a ms-reservas los IDs lógicos, duración y costo
 * para ejecutar la validación de no colisión horaria tripartita (RN-03).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilidadRecursoDTO {

    private Long profesionalId;
    private Long servicioId;
    private Integer duracionMinutos;
    private BigDecimal costo;
    private Long sedeId;
    private Long consultorioFijoId;
    private List<Long> equipamientosIds;
}
