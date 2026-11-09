package utn.frc.backend.mediturnos.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando un recurso físico requerido (Sede, Consultorio fijo N:1 o
 * Equipamiento de diagnóstico N:N) se encuentra inactivo o en mantenimiento (operativo = false) (RN-02 / CV-03).
 * Retorna por defecto HTTP 409 Conflict (y soporta HTTP 422 Unprocessable Entity).
 */
public class RecursoNoDisponibleException extends MediTurnosBaseException {

    private static final String DEFAULT_RULE_CODE = "RN-02";

    public RecursoNoDisponibleException(String message) {
        super(message, HttpStatus.CONFLICT, DEFAULT_RULE_CODE);
    }

    public RecursoNoDisponibleException(String message, HttpStatus status) {
        super(message, status, DEFAULT_RULE_CODE);
    }

    public RecursoNoDisponibleException(String message, HttpStatus status, String ruleCode) {
        super(message, status, ruleCode);
    }
}
