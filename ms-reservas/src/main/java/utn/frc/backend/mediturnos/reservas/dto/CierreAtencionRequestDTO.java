package utn.frc.backend.mediturnos.reservas.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import utn.frc.backend.mediturnos.common.enums.EstadoTurno;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CierreAtencionRequestDTO {

    @NotNull(message = "El resultado de la atención médica es obligatorio")
    private EstadoTurno resultado; // ATENDIDO o AUSENTE
}
