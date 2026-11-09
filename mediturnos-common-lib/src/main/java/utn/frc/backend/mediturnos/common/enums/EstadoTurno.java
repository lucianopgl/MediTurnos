package utn.frc.backend.mediturnos.common.enums;

/**
 * Máquina de estados del Turno Médico (Sección 3.2 del README.md y CHECK constraint en init-db.sql).
 */
public enum EstadoTurno {
    RESERVADO,
    CONFIRMADO,
    CANCELADO_PACIENTE,
    CANCELADO_PENALIZADO,
    PENDIENTE_REPROGRAMACION,
    ATENDIDO,
    AUSENTE;

    /**
     * Indica si el turno en este estado bloquea recursos físicos y agenda para la validación
     * de no colisión horaria tripartita (RN-03).
     *
     * @return true únicamente si el estado es RESERVADO o CONFIRMADO.
     */
    public boolean ocupaRecursos() {
        return this == RESERVADO || this == CONFIRMADO;
    }

    /**
     * Indica si el turno puede ser cancelado por el paciente (RN-05) o afectado por un
     * bloqueo puntual de agenda médica (RN-06).
     *
     * @return true si el turno está activo (RESERVADO o CONFIRMADO).
     */
    public boolean esCancelableOBloqueable() {
        return this == RESERVADO || this == CONFIRMADO;
    }
}
