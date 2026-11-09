package utn.frc.backend.mediturnos.common.enums;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Días de la semana para la plantilla horaria parametrizada del profesional
 * (tabla schema_catalogo.horarios_profesional — RN-01).
 */
public enum DiaSemana {
    LUNES,
    MARTES,
    MIERCOLES,
    JUEVES,
    VIERNES,
    SABADO,
    DOMINGO;

    /**
     * Convierte un {@link DayOfWeek} de Java Time al enum de dominio en español sin tildes.
     */
    public static DiaSemana from(DayOfWeek dayOfWeek) {
        Objects.requireNonNull(dayOfWeek, "El día de la semana (DayOfWeek) no puede ser nulo");
        return switch (dayOfWeek) {
            case MONDAY -> LUNES;
            case TUESDAY -> MARTES;
            case WEDNESDAY -> MIERCOLES;
            case THURSDAY -> JUEVES;
            case FRIDAY -> VIERNES;
            case SATURDAY -> SABADO;
            case SUNDAY -> DOMINGO;
        };
    }

    /**
     * Obtiene el {@link DiaSemana} correspondiente a una fecha {@link LocalDate} (RN-01).
     */
    public static DiaSemana from(LocalDate fecha) {
        Objects.requireNonNull(fecha, "La fecha no puede ser nula");
        return from(fecha.getDayOfWeek());
    }
}
