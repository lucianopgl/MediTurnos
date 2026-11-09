package utn.frc.backend.mediturnos.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepción base abstracta para el dominio de MediTurnos API.
 * Centraliza el código de estado HTTP y el código de regla de negocio comprometida (RN-01 a RN-08).
 */
@Getter
public abstract class MediTurnosBaseException extends RuntimeException {

    private final HttpStatus status;
    private final String ruleCode;

    protected MediTurnosBaseException(String message, HttpStatus status, String ruleCode) {
        super(message);
        this.status = status;
        this.ruleCode = ruleCode;
    }

    protected MediTurnosBaseException(String message, HttpStatus status, String ruleCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.ruleCode = ruleCode;
    }
}
