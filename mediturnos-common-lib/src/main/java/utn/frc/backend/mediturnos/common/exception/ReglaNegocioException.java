package utn.frc.backend.mediturnos.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada ante el incumplimiento de reglas estáticas o parametrizadas de negocio (RN-01).
 * Ejemplos: Profesional inactivo, servicio no habilitado en PROFESIONAL_SERVICIOS,
 * horario fuera de la plantilla semanal HORARIOS_PROFESIONAL o bloqueo vigente en BLOQUEOS_PROFESIONAL.
 * Retorna por defecto HTTP 422 Unprocessable Entity.
 */
public class ReglaNegocioException extends MediTurnosBaseException {

    private static final String DEFAULT_RULE_CODE = "RN-01";

    public ReglaNegocioException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, DEFAULT_RULE_CODE);
    }

    public ReglaNegocioException(String message, String ruleCode) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, ruleCode);
    }

    public ReglaNegocioException(String message, HttpStatus status, String ruleCode) {
        super(message, status, ruleCode);
    }
}
