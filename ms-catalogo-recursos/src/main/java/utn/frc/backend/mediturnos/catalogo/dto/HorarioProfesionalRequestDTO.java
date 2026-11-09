package utn.frc.backend.mediturnos.catalogo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import utn.frc.backend.mediturnos.common.enums.DiaSemana;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorarioProfesionalRequestDTO {

    @NotNull(message = "El día de la semana es obligatorio")
    private DiaSemana diaSemana;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    @Builder.Default
    private Boolean activo = true;
}
