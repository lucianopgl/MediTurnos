package utn.frc.backend.mediturnos.reservas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import utn.frc.backend.mediturnos.common.enums.EstadoTurno;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoResponseDTO {

    private Long id;
    private Long pacienteId;
    private String pacienteNombreCompleto;
    private Long profesionalId;
    private Long servicioId;
    private Long consultorioId;
    private Long sedeId;
    private List<Long> equipamientosIds;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private BigDecimal costoAcordado;
    private EstadoTurno estado;
    private Boolean pagado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
