package utn.frc.backend.mediturnos.catalogo.dto;

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
public class HorarioProfesionalResponseDTO {

    private Long id;
    private Long profesionalId;
    private DiaSemana diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Boolean activo;
}
