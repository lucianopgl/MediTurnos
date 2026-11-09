package utn.frc.backend.mediturnos.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Contrato sustantivo para el alta de un bloqueo puntual de agenda médica
 * en POST /api/catalogo/profesionales/{id}/bloqueos (FUNC-10 / RN-06).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloqueoAgendaRequestDTO {

    @NotNull(message = "La fecha del bloqueo es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio del bloqueo es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin del bloqueo es obligatoria")
    private LocalTime horaFin;

    @NotBlank(message = "El motivo del bloqueo es obligatorio")
    private String motivo;
}
