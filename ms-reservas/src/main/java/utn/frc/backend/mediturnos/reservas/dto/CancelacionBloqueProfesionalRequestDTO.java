package utn.frc.backend.mediturnos.reservas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelacionBloqueProfesionalRequestDTO {

    @NotNull(message = "El profesionalId es obligatorio")
    private Long profesionalId;

    @NotNull(message = "La fecha del bloqueo es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    @NotBlank(message = "El motivo del bloqueo es obligatorio")
    private String motivo;
}
