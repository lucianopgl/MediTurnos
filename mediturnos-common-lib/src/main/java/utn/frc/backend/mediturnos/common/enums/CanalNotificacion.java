package utn.frc.backend.mediturnos.common.enums;

/**
 * Canales de comunicación soportados para el despacho de notificaciones externas
 * (tabla schema_reservas.notificaciones — RN-08).
 */
public enum CanalNotificacion {
    TELEGRAM,
    EMAIL,
    /** Evento en Google Calendar con invitación por email al paciente (sendUpdates=all). */
    GOOGLE_CALENDAR
}
