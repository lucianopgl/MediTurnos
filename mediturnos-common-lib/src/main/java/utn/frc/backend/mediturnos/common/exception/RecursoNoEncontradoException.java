package utn.frc.backend.mediturnos.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando una entidad solicitada por ID o por keycloak_sub no existe
 * (Sede, Consultorio, Especialidad, Profesional, Servicio, Equipamiento, Paciente o Turno).
 * Retorna HTTP 404 Not Found.
 */
public class RecursoNoEncontradoException extends MediTurnosBaseException {

    private static final String DEFAULT_RULE_CODE = "NOT_FOUND";

    public RecursoNoEncontradoException(String message) {
        super(message, HttpStatus.NOT_FOUND, DEFAULT_RULE_CODE);
    }

    public RecursoNoEncontradoException(String message, String ruleCode) {
        super(message, HttpStatus.NOT_FOUND, ruleCode);
    }
}
