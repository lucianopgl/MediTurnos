package utn.frc.backend.mediturnos.reservas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import utn.frc.backend.mediturnos.common.enums.CanalNotificacion;
import utn.frc.backend.mediturnos.common.enums.EstadoEnvioNotificacion;
import utn.frc.backend.mediturnos.common.enums.TipoAlertaNotificacion;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionResponseDTO {

    private Long id;
    private Long turnoId;
    private Long pacienteId;
    private CanalNotificacion canal;
    private String destinatario;
    private TipoAlertaNotificacion tipoAlerta;
    private EstadoEnvioNotificacion estadoEnvio;
    private String detalleMensaje;
    private LocalDateTime fechaEnvio;
}
