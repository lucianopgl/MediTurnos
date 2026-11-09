package utn.frc.backend.mediturnos.catalogo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.frc.backend.mediturnos.catalogo.entity.*;
import utn.frc.backend.mediturnos.catalogo.repository.BloqueoProfesionalRepository;
import utn.frc.backend.mediturnos.catalogo.repository.HorarioProfesionalRepository;
import utn.frc.backend.mediturnos.common.dto.CompatibilidadRecursoDTO;
import utn.frc.backend.mediturnos.common.enums.DiaSemana;
import utn.frc.backend.mediturnos.common.exception.CoherenciaSedeException;
import utn.frc.backend.mediturnos.common.exception.RecursoNoDisponibleException;
import utn.frc.backend.mediturnos.common.exception.ReglaNegocioException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompatibilidadService {

    private final ProfesionalService profesionalService;
    private final ServicioService servicioService;
    private final HorarioProfesionalRepository horarioProfesionalRepository;
    private final BloqueoProfesionalRepository bloqueoProfesionalRepository;

    /**
     * Evalúa las reglas estáticas y parametrizadas RN-01 y RN-02 para un intento de reserva
     * y retorna el snapshot de recursos requeridos para validar no colisión (RN-03) en ms-reservas.
     */
    @Transactional(readOnly = true)
    public CompatibilidadRecursoDTO evaluarCompatibilidad(Long profesionalId,
                                                         Long servicioId,
                                                         LocalDate fecha,
                                                         LocalTime horaInicio) {
        log.info("Evaluando compatibilidad: profesionalId={}, servicioId={}, fecha={}, horaInicio={}",
                profesionalId, servicioId, fecha, horaInicio);

        // =====================================================================
        // REGLA RN-01: Habilitación y plantilla horaria del profesional
        // =====================================================================

        Profesional profesional = profesionalService.buscarEntidadPorId(profesionalId);
        if (Boolean.FALSE.equals(profesional.getActivo())) {
            throw new ReglaNegocioException("El profesional " + profesional.getNombre() + " "
                    + profesional.getApellido() + " se encuentra inactivo");
        }

        Servicio servicio = servicioService.buscarEntidadPorId(servicioId);
        if (Boolean.FALSE.equals(servicio.getActivo())) {
            throw new ReglaNegocioException("El servicio médico '" + servicio.getNombre() + "' se encuentra inactivo");
        }

        boolean habilitado = profesional.getServiciosHabilitados().stream()
                .anyMatch(s -> s.getId().equals(servicioId));
        if (!habilitado) {
            throw new ReglaNegocioException("El profesional no está habilitado para realizar el servicio '"
                    + servicio.getNombre() + "'");
        }

        LocalTime horaFin = horaInicio.plusMinutes(servicio.getDuracionMinutos());
        DiaSemana diaSemana = DiaSemana.from(fecha);

        // Verificar si la franja [horaInicio, horaFin] está dentro de algún bloque activo de la plantilla semanal
        List<HorarioProfesional> horariosDia = horarioProfesionalRepository
                .findByProfesionalIdAndDiaSemanaAndActivoTrue(profesionalId, diaSemana);

        boolean dentroDePlantilla = horariosDia.stream().anyMatch(h ->
                !horaInicio.isBefore(h.getHoraInicio()) && !horaFin.isAfter(h.getHoraFin())
        );

        if (!dentroDePlantilla) {
            throw new ReglaNegocioException("El horario solicitado (" + diaSemana + " " + horaInicio
                    + " a " + horaFin + ") se encuentra fuera de la plantilla semanal de atención del profesional");
        }

        // Verificar que no existan bloqueos de agenda en la fecha y franja solicitada
        List<BloqueoProfesional> solapamientosBloqueo = bloqueoProfesionalRepository
                .findSolapamientos(profesionalId, fecha, horaInicio, horaFin);

        if (!solapamientosBloqueo.isEmpty()) {
            BloqueoProfesional bloqueo = solapamientosBloqueo.getFirst();
            throw new ReglaNegocioException("La agenda del profesional se encuentra bloqueada en esa fecha y franja ("
                    + bloqueo.getHoraInicio() + " a " + bloqueo.getHoraFin() + "): " + bloqueo.getMotivo());
        }

        // =====================================================================
        // REGLA RN-02: Estado operativo y coherencia de sede física
        // =====================================================================

        Consultorio consultorioFijo = profesional.getConsultorioFijo();
        Sede sede = consultorioFijo.getSede();

        if (Boolean.FALSE.equals(sede.getActiva())) {
            throw new RecursoNoDisponibleException("La sede '" + sede.getNombre() + "' se encuentra inactiva",
                    HttpStatus.CONFLICT);
        }

        if (Boolean.FALSE.equals(consultorioFijo.getOperativo())) {
            throw new RecursoNoDisponibleException("El consultorio fijo asignado '" + consultorioFijo.getCodigoNombre()
                    + "' se encuentra en mantenimiento (no operativo)", HttpStatus.CONFLICT);
        }

        List<Long> equipamientosIds = servicio.getEquipamientosRequeridos().stream()
                .peek(eq -> {
                    // Coherencia física de sede
                    if (!sede.getId().equals(eq.getSede().getId())) {
                        throw new CoherenciaSedeException("Incoherencia física: el equipamiento '" + eq.getNombre()
                                + "' pertenece a la sede id=" + eq.getSede().getId()
                                + ", mientras que el consultorio pertenece a la sede id=" + sede.getId());
                    }
                    // Estado operativo
                    if (Boolean.FALSE.equals(eq.getOperativo())) {
                        throw new RecursoNoDisponibleException("El equipamiento requerido '" + eq.getNombre()
                                + "' (" + eq.getCodigoInventario() + ") se encuentra en mantenimiento (no operativo)", HttpStatus.CONFLICT);
                    }
                })
                .map(Equipamiento::getId)
                .sorted()
                .toList();

        log.info("Compatibilidad de catálogo validada exitosamente para profesionalId={}, servicioId={}",
                profesionalId, servicioId);

        return CompatibilidadRecursoDTO.builder()
                .profesionalId(profesional.getId())
                .servicioId(servicio.getId())
                .duracionMinutos(servicio.getDuracionMinutos())
                .costo(servicio.getCosto())
                .sedeId(sede.getId())
                .consultorioFijoId(consultorioFijo.getId())
                .equipamientosIds(equipamientosIds)
                .build();
    }
}
