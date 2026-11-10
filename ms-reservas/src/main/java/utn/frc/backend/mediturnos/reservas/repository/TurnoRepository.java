package utn.frc.backend.mediturnos.reservas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import utn.frc.backend.mediturnos.reservas.entity.Turno;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Long>, JpaSpecificationExecutor<Turno> {

    // 1. Colisión de Profesional (RN-03)
    @Query("""
        SELECT COUNT(t) > 0 FROM Turno t
        WHERE t.fecha = :fecha
          AND t.profesionalId = :profesionalId
          AND t.estado IN (utn.frc.backend.mediturnos.common.enums.EstadoTurno.RESERVADO, utn.frc.backend.mediturnos.common.enums.EstadoTurno.CONFIRMADO)
          AND t.horaInicio < :horaFin AND t.horaFin > :horaInicio
          AND (:turnoExcluidoId IS NULL OR t.id <> :turnoExcluidoId)
    """)
    boolean existeColisionProfesional(
            @Param("profesionalId") Long profesionalId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("turnoExcluidoId") Long turnoExcluidoId
    );

    // 2. Colisión de Consultorio Fijo Compartido (N:1) (RN-03)
    @Query("""
        SELECT COUNT(t) > 0 FROM Turno t
        WHERE t.fecha = :fecha
          AND t.consultorioId = :consultorioId
          AND t.estado IN (utn.frc.backend.mediturnos.common.enums.EstadoTurno.RESERVADO, utn.frc.backend.mediturnos.common.enums.EstadoTurno.CONFIRMADO)
          AND t.horaInicio < :horaFin AND t.horaFin > :horaInicio
          AND (:turnoExcluidoId IS NULL OR t.id <> :turnoExcluidoId)
    """)
    boolean existeColisionConsultorio(
            @Param("consultorioId") Long consultorioId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("turnoExcluidoId") Long turnoExcluidoId
    );

    // 3. Colisión de Equipamientos de Diagnóstico (N:N) (RN-03)
    @Query("""
        SELECT COUNT(DISTINCT te.equipamientoId) > 0 FROM Turno t
        JOIN t.equipamientos te
        WHERE t.fecha = :fecha
          AND te.equipamientoId IN :equipamientosIds
          AND t.estado IN (utn.frc.backend.mediturnos.common.enums.EstadoTurno.RESERVADO, utn.frc.backend.mediturnos.common.enums.EstadoTurno.CONFIRMADO)
          AND t.horaInicio < :horaFin AND t.horaFin > :horaInicio
          AND (:turnoExcluidoId IS NULL OR t.id <> :turnoExcluidoId)
    """)
    boolean existeColisionEquipamientos(
            @Param("equipamientosIds") List<Long> equipamientosIds,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("turnoExcluidoId") Long turnoExcluidoId
    );

    // 4. Colisión de Paciente (RN-03)
    @Query("""
        SELECT COUNT(t) > 0 FROM Turno t
        WHERE t.fecha = :fecha
          AND t.paciente.id = :pacienteId
          AND t.estado IN (utn.frc.backend.mediturnos.common.enums.EstadoTurno.RESERVADO, utn.frc.backend.mediturnos.common.enums.EstadoTurno.CONFIRMADO)
          AND t.horaInicio < :horaFin AND t.horaFin > :horaInicio
          AND (:turnoExcluidoId IS NULL OR t.id <> :turnoExcluidoId)
    """)
    boolean existeColisionPaciente(
            @Param("pacienteId") Long pacienteId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("turnoExcluidoId") Long turnoExcluidoId
    );

    // Turnos afectados por bloqueo puntual de agenda médica (RN-06)
    @Query("""
        SELECT t FROM Turno t
        WHERE t.profesionalId = :profesionalId
          AND t.fecha = :fecha
          AND t.estado IN (utn.frc.backend.mediturnos.common.enums.EstadoTurno.RESERVADO, utn.frc.backend.mediturnos.common.enums.EstadoTurno.CONFIRMADO)
          AND t.horaInicio < :horaFin AND t.horaFin > :horaInicio
    """)
    List<Turno> buscarTurnosAfectadosPorBloqueo(
            @Param("profesionalId") Long profesionalId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin
    );
}
