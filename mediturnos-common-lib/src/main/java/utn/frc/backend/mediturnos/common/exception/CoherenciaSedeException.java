package utn.frc.backend.mediturnos.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando el consultorio fijo del profesional (N:1) y alguno de los equipamientos
 * requeridos por el servicio médico (N:N) pertenecen a distintas sedes físicas (sedeId diferente) (RN-02).
 * Retorna HTTP 422 Unprocessable Entity.
 */
public class CoherenciaSedeException extends MediTurnosBaseException {

    private static final String DEFAULT_RULE_CODE = "RN-02";

    public CoherenciaSedeException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, DEFAULT_RULE_CODE);
    }

    public CoherenciaSedeException(String message, HttpStatus status) {
        super(message, status, DEFAULT_RULE_CODE);
    }
}
