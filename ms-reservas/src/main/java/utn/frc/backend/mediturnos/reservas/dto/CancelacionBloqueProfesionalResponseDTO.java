package utn.frc.backend.mediturnos.reservas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelacionBloqueProfesionalResponseDTO {

    private Long profesionalId;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String motivo;
    private Integer cantidadTurnosAfectados;
    private List<Long> turnosAfectadosIds;
}
