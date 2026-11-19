package utn.frc.backend.mediturnos.reservas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import utn.frc.backend.mediturnos.common.enums.CanalNotificacion;
import utn.frc.backend.mediturnos.common.enums.EstadoEnvioNotificacion;
import utn.frc.backend.mediturnos.common.enums.EstadoTurno;
import utn.frc.backend.mediturnos.common.enums.TipoAlertaNotificacion;
import utn.frc.backend.mediturnos.reservas.client.GoogleCalendarClient;
import utn.frc.backend.mediturnos.reservas.dto.NotificacionResponseDTO;
import utn.frc.backend.mediturnos.reservas.entity.Notificacion;
import utn.frc.backend.mediturnos.reservas.entity.Paciente;
import utn.frc.backend.mediturnos.reservas.entity.Turno;
import utn.frc.backend.mediturnos.reservas.repository.NotificacionRepository;

import java.util.List;
import java.util.Map;

/**
 * Servicio resiliente de despacho y auditoría de notificaciones externas (RN-08).
 * Canales: Telegram Bot API (mensaje) y Google Calendar API (evento + invitación por email al paciente).
 * Captura cualquier excepción de la API externa o de persistencia de auditoría
 * para que un fallo en la notificación jamás revierta la transacción de negocio del turno.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private static final int MAX_DETALLE_LENGTH = 500;

    private final NotificacionRepository notificacionRepository;
    private final RestClient notificacionRestClient;
    private final GoogleCalendarClient googleCalendarClient;

    @Value("${notificaciones.api.bot-token:dummy_token_desarrollo}")
    private String botToken;

    @Transactional
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

        registrarAuditoria(turno, paciente, CanalNotificacion.TELEGRAM, destinatario, tipoAlerta, estadoEnvio, detalleFinal);

        sincronizarGoogleCalendarResiliente(turno, paciente, tipoAlerta, mensaje);
    }

    /**
     * Refleja el turno en Google Calendar invitando al paciente por email (RN-08: nunca propaga errores).
     * <ul>
     *   <li>RESERVA / CONFIRMACION / REPROGRAMACION (turno activo): crea o actualiza el evento.</li>
     *   <li>CANCELACION: cancela el evento.</li>
     *   <li>REPROGRAMACION con turno PENDIENTE_REPROGRAMACION: no toca el calendario (aún no hay nuevo horario).</li>
     * </ul>
     */
    void sincronizarGoogleCalendarResiliente(Turno turno, Paciente paciente, TipoAlertaNotificacion tipoAlerta, String mensaje) {
        if (turno == null || turno.getId() == null || paciente == null) {
            return;
        }
        if (tipoAlerta == TipoAlertaNotificacion.REPROGRAMACION && turno.getEstado() == EstadoTurno.PENDIENTE_REPROGRAMACION) {
            return;
        }

        boolean esCancelacion = tipoAlerta == TipoAlertaNotificacion.CANCELACION;
        String destinatario = paciente.getEmail();
        EstadoEnvioNotificacion estadoEnvio;
        String detalle;

        try {
            if (!googleCalendarClient.isEnabled()) {
                // Modo simulado: mismo criterio que Telegram para poder verificar RN-08 sin credenciales
                if (destinatario != null && (destinatario.contains("error") || destinatario.contains("fallo"))) {
                    throw new IllegalStateException("Fallo simulado en conexión con Google Calendar API (RN-08)");
                }
                log.info("[RN-08 SIMULADO] Google Calendar {} turno #{} para {}",
                        esCancelacion ? "cancelación de evento" : "evento/invitación", turno.getId(), destinatario);
            } else if (esCancelacion) {
                googleCalendarClient.cancelarEvento(turno);
            } else {
                googleCalendarClient.crearOActualizarEvento(turno, paciente, mensaje);
            }
            estadoEnvio = EstadoEnvioNotificacion.ENVIADO;
            detalle = (esCancelacion ? "Evento cancelado: " : "Evento e invitación por email: ")
                    + googleCalendarClient.eventIdPara(turno)
                    + (googleCalendarClient.isEnabled() ? "" : " [SIMULADO]");
        } catch (Exception ex) {
            log.warn("[RN-08 DEGRADACIÓN CONTROLADA] Error al sincronizar Google Calendar para turno #{}: {}. El turno se confirma normalmente.",
                    turno.getId(), ex.getMessage());
            estadoEnvio = EstadoEnvioNotificacion.ERROR;
            detalle = "FALLO_ENVIO: " + ex.getMessage();
        }

        registrarAuditoria(turno, paciente, CanalNotificacion.GOOGLE_CALENDAR, destinatario, tipoAlerta, estadoEnvio, detalle);
    }

    private void registrarAuditoria(Turno turno, Paciente paciente, CanalNotificacion canal, String destinatario,
                                    TipoAlertaNotificacion tipoAlerta, EstadoEnvioNotificacion estadoEnvio, String detalle) {
        if (detalle != null && detalle.length() > MAX_DETALLE_LENGTH) {
            detalle = detalle.substring(0, MAX_DETALLE_LENGTH);
        }

        try {
            Notificacion notificacion = Notificacion.builder()
                    .turno(turno)
                    .paciente(paciente)
                    .canal(canal)
                    .destinatario(destinatario)
                    .tipoAlerta(tipoAlerta)
                    .estadoEnvio(estadoEnvio)
                    .detalleMensaje(detalle)
                    .build();

            notificacionRepository.save(notificacion);
        } catch (Exception persistEx) {
            log.error("[RN-08] Error al persistir auditoría de notificación {} para turno #{}: {}",
                    canal, turno != null ? turno.getId() : null, persistEx.getMessage());
        }
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
        if (pacienteId != null && estado != null) {
            list = notificacionRepository.findByPacienteIdAndEstadoEnvioOrderByFechaEnvioDesc(pacienteId, estado);
        } else if (pacienteId != null) {
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
