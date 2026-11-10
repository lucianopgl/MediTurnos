package utn.frc.backend.mediturnos.reservas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import utn.frc.backend.mediturnos.common.dto.ErrorResponseDTO;
import utn.frc.backend.mediturnos.common.enums.CanalNotificacion;
import utn.frc.backend.mediturnos.common.enums.EstadoEnvioNotificacion;
import utn.frc.backend.mediturnos.common.enums.TipoAlertaNotificacion;
import utn.frc.backend.mediturnos.reservas.dto.NotificacionResponseDTO;
import utn.frc.backend.mediturnos.reservas.entity.Notificacion;
import utn.frc.backend.mediturnos.reservas.entity.Paciente;
import utn.frc.backend.mediturnos.reservas.entity.Turno;
import utn.frc.backend.mediturnos.reservas.repository.NotificacionRepository;

import java.util.List;
import java.util.Map;

/**
 * Servicio resiliente de despacho y auditoría de notificaciones externas (RN-08).
 * Su ejecución se aísla con REQUIRES_NEW para que un fallo en la API externa nunca
 * revierta la transacción de negocio del turno.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final RestClient notificacionRestClient;

    @Value("${notificaciones.api.bot-token:dummy_token_desarrollo}")
    private String botToken;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enviarNotificacionResiliente(Turno turno, Paciente paciente, TipoAlertaNotificacion tipoAlerta, String mensaje) {
        EstadoEnvioNotificacion estadoEnvio;
        String detalleFinal = mensaje;
        String destinatario = (paciente.getTelefonoWhatsapp() != null && !paciente.getTelefonoWhatsapp().isBlank())
                ? paciente.getTelefonoWhatsapp()
                : paciente.getEmail();

        try {
            if ("dummy_token_desarrollo".equalsIgnoreCase(botToken)) {
                // Modo Simulación Controlada (Opción A):
                // Si el destinatario contiene "error" o "fallo", se simula fallo de red para verificar RN-08
                if (destinatario != null && (destinatario.contains("error") || destinatario.contains("fallo"))) {
                    throw new RestClientException("Fallo simulado en conexión con API externa de mensajería (RN-08)");
                }
                log.info("[RN-08 SIMULADO] Notificación {} enviada con éxito a {}", tipoAlerta, destinatario);
                estadoEnvio = EstadoEnvioNotificacion.ENVIADO;
            } else {
                // Modo Integración Real HTTP
                ejecutarLlamadaExterna(destinatario, mensaje);
                estadoEnvio = EstadoEnvioNotificacion.ENVIADO;
            }
        } catch (Exception ex) {
            log.warn("[RN-08 DEGRADACIÓN CONTROLADA] Error al enviar notificación a {}: {}. El turno se confirma normalmente.",
                    destinatario, ex.getMessage());
            estadoEnvio = EstadoEnvioNotificacion.ERROR;
            detalleFinal = "FALLO_ENVIO: " + ex.getMessage();
        }

        Notificacion notificacion = Notificacion.builder()
                .turno(turno)
                .paciente(paciente)
                .canal(CanalNotificacion.TELEGRAM)
                .destinatario(destinatario)
                .tipoAlerta(tipoAlerta)
                .estadoEnvio(estadoEnvio)
                .detalleMensaje(detalleFinal)
                .build();

        notificacionRepository.save(notificacion);
    }

    private void ejecutarLlamadaExterna(String destinatario, String mensaje) {
        Map<String, Object> payload = Map.of(
                "chat_id", destinatario,
                "text", mensaje
        );

        notificacionRestClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponseDTO> listarNotificaciones(EstadoEnvioNotificacion estado, Long pacienteId) {
        List<Notificacion> list;
        if (pacienteId != null) {
            list = notificacionRepository.findByPacienteIdOrderByFechaEnvioDesc(pacienteId);
        } else if (estado != null) {
            list = notificacionRepository.findByEstadoEnvioOrderByFechaEnvioDesc(estado);
        } else {
            list = notificacionRepository.findAllByOrderByFechaEnvioDesc();
        }

        return list.stream()
                .map(n -> NotificacionResponseDTO.builder()
                        .id(n.getId())
                        .turnoId(n.getTurno() != null ? n.getTurno().getId() : null)
                        .pacienteId(n.getPaciente().getId())
                        .canal(n.getCanal())
                        .destinatario(n.getDestinatario())
                        .tipoAlerta(n.getTipoAlerta())
                        .estadoEnvio(n.getEstadoEnvio())
                        .detalleMensaje(n.getDetalleMensaje())
                        .fechaEnvio(n.getFechaEnvio())
                        .build())
                .toList();
    }
}
