package utn.frc.backend.mediturnos.reservas.dto;

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
public class TurnoRequestDTO {

    private Long pacienteId;

    @NotNull(message = "El profesionalId es obligatorio")
    private Long profesionalId;

    @NotNull(message = "El servicioId es obligatorio")
    private Long servicioId;

    @NotNull(message = "La fecha del turno es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio del turno es obligatoria")
    private LocalTime horaInicio;
}
