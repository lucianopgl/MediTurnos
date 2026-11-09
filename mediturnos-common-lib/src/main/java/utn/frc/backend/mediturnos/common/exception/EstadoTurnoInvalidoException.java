package utn.frc.backend.mediturnos.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada ante transiciones inválidas en la máquina de estados del turno
 * (ej. pagar un turno ya pagado o cancelado, cancelar un turno ya atendido, reprogramar un turno
 * que no está en PENDIENTE_REPROGRAMACION) o monto de pago distinto al costoAcordado (RN-04, RN-05, RN-07).
 * Retorna por defecto HTTP 409 Conflict (y soporta HTTP 400 Bad Request para montos inválidos).
 */
public class EstadoTurnoInvalidoException extends MediTurnosBaseException {

    private static final String DEFAULT_RULE_CODE = "RN-04";

    public EstadoTurnoInvalidoException(String message) {
        super(message, HttpStatus.CONFLICT, DEFAULT_RULE_CODE);
    }

    public EstadoTurnoInvalidoException(String message, String ruleCode) {
        super(message, HttpStatus.CONFLICT, ruleCode);
    }

    public EstadoTurnoInvalidoException(String message, HttpStatus status, String ruleCode) {
        super(message, status, ruleCode);
    }
}
