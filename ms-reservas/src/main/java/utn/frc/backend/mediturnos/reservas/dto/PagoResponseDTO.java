package utn.frc.backend.mediturnos.reservas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import utn.frc.backend.mediturnos.common.enums.EstadoPago;
import utn.frc.backend.mediturnos.common.enums.EstadoTurno;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoResponseDTO {

    private Long id;
    private Long turnoId;
    private String comprobanteReferencia;
    private String medioPago;
    private BigDecimal monto;
    private EstadoPago estadoPago;
    private LocalDateTime fechaPago;
    private EstadoTurno estadoTurnoActual;
}
