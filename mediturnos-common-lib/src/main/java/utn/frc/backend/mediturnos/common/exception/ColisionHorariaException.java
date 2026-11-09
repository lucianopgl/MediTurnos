package utn.frc.backend.mediturnos.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada por ms-reservas cuando existe solapamiento horario en [horaInicio, horaFin)
 * con algún turno en estado RESERVADO o CONFIRMADO sobre el mismo profesionalId, consultorioId (N:1),
 * equipamientoId (N:N) o pacienteId (RN-03 / CV-02).
 * Retorna HTTP 409 Conflict.
 */
public class ColisionHorariaException extends MediTurnosBaseException {

    private static final String DEFAULT_RULE_CODE = "RN-03";

    public ColisionHorariaException(String message) {
        super(message, HttpStatus.CONFLICT, DEFAULT_RULE_CODE);
    }

    public ColisionHorariaException(String message, String ruleCode) {
        super(message, HttpStatus.CONFLICT, ruleCode);
    }
}
