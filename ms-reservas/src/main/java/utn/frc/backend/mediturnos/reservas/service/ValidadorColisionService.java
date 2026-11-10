package utn.frc.backend.mediturnos.reservas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utn.frc.backend.mediturnos.common.exception.ColisionHorariaException;
import utn.frc.backend.mediturnos.reservas.repository.TurnoRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Motor de validación de no colisión horaria tripartita y de paciente (RN-03 / CV-02).
 * Evalúa solapamiento de intervalos abiertos [horaInicio, horaFin) sobre:
 * 1) Profesional
 * 2) Consultorio fijo compartido (N:1)
 * 3) Equipamientos médicos de diagnóstico (N:N)
 * 4) Paciente
 */
@Service
@RequiredArgsConstructor
public class ValidadorColisionService {

    private final TurnoRepository turnoRepository;

    public void validarNoColision(Long pacienteId, Long profesionalId, Long consultorioId,
                                  List<Long> equipamientosIds, LocalDate fecha,
                                  LocalTime horaInicio, LocalTime horaFin, Long turnoExcluidoId) {

        // 1. Colisión de Profesional
        if (turnoRepository.existeColisionProfesional(profesionalId, fecha, horaInicio, horaFin, turnoExcluidoId)) {
            throw new ColisionHorariaException("El profesional seleccionado ya cuenta con un turno reservado o confirmado en esa franja horaria.");
        }

        // 2. Colisión de Consultorio Fijo Compartido (N:1)
        if (turnoRepository.existeColisionConsultorio(consultorioId, fecha, horaInicio, horaFin, turnoExcluidoId)) {
            throw new ColisionHorariaException("El consultorio asignado (fijo compartido) se encuentra ocupado por otro turno en esa franja horaria.");
        }

        // 3. Colisión de Equipamientos Diagnóstico (N:N)
        if (equipamientosIds != null && !equipamientosIds.isEmpty()) {
            if (turnoRepository.existeColisionEquipamientos(equipamientosIds, fecha, horaInicio, horaFin, turnoExcluidoId)) {
                throw new ColisionHorariaException("Uno o más equipamientos de diagnóstico requeridos ya están reservados en esa franja horaria.");
            }
        }

        // 4. Colisión de Paciente
        if (turnoRepository.existeColisionPaciente(pacienteId, fecha, horaInicio, horaFin, turnoExcluidoId)) {
            throw new ColisionHorariaException("El paciente ya posee un turno reservado o confirmado en el mismo horario.");
        }
    }
}
