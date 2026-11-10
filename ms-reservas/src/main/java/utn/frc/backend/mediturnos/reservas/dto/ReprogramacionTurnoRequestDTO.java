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
public class ReprogramacionTurnoRequestDTO {

    @NotNull(message = "La nueva fecha es obligatoria")
    private LocalDate nuevaFecha;

    @NotNull(message = "La nueva hora de inicio es obligatoria")
    private LocalTime nuevaHoraInicio;
}
