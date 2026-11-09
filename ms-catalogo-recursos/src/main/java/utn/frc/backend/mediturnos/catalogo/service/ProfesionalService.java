package utn.frc.backend.mediturnos.catalogo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.frc.backend.mediturnos.catalogo.dto.*;
import utn.frc.backend.mediturnos.catalogo.entity.*;
import utn.frc.backend.mediturnos.catalogo.repository.BloqueoProfesionalRepository;
import utn.frc.backend.mediturnos.catalogo.repository.HorarioProfesionalRepository;
import utn.frc.backend.mediturnos.catalogo.repository.ProfesionalRepository;
import utn.frc.backend.mediturnos.common.dto.BloqueoAgendaRequestDTO;
import utn.frc.backend.mediturnos.common.exception.CoherenciaSedeException;
import utn.frc.backend.mediturnos.common.exception.RecursoNoDisponibleException;
import utn.frc.backend.mediturnos.common.exception.RecursoNoEncontradoException;
import utn.frc.backend.mediturnos.common.exception.ReglaNegocioException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfesionalService {

    private final ProfesionalRepository profesionalRepository;
    private final EspecialidadService especialidadService;
    private final ConsultorioService consultorioService;
    private final ServicioService servicioService;
    private final HorarioProfesionalRepository horarioProfesionalRepository;
    private final BloqueoProfesionalRepository bloqueoProfesionalRepository;

    @Transactional
    public ProfesionalResponseDTO registrarProfesional(ProfesionalRequestDTO request) {
        if (profesionalRepository.existsByKeycloakSub(request.getKeycloakSub())) {
            throw new ReglaNegocioException("Ya existe un profesional asociado al keycloak_sub: " + request.getKeycloakSub());
        }
        if (profesionalRepository.existsByMatriculaIgnoreCase(request.getMatricula())) {
            throw new ReglaNegocioException("Ya existe un profesional con la matrícula: " + request.getMatricula());
        }
        if (profesionalRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ReglaNegocioException("Ya existe un profesional con el email: " + request.getEmail());
        }

        Especialidad especialidad = especialidadService.buscarEntidadPorId(request.getEspecialidadId());
        if (Boolean.FALSE.equals(especialidad.getActiva())) {
            throw new RecursoNoDisponibleException("No se puede asignar una especialidad inactiva al profesional");
        }

        Consultorio consultorio = consultorioService.buscarEntidadPorId(request.getConsultorioFijoId());
        if (Boolean.FALSE.equals(consultorio.getSede().getActiva())) {
            throw new RecursoNoDisponibleException("La sede del consultorio fijo asignado no está activa");
        }

        Set<Servicio> servicios = new HashSet<>();
        if (request.getServiciosIds() != null && !request.getServiciosIds().isEmpty()) {
            servicios.addAll(servicioService.buscarEntidadesPorIds(request.getServiciosIds()));
            validarCoherenciaSede(consultorio, servicios);
        }

        Profesional profesional = Profesional.builder()
                .keycloakSub(request.getKeycloakSub().trim())
                .especialidad(especialidad)
                .consultorioFijo(consultorio)
                .matricula(request.getMatricula().trim())
                .nombre(request.getNombre().trim())
                .apellido(request.getApellido().trim())
                .email(request.getEmail().trim().toLowerCase())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .serviciosHabilitados(servicios)
                .build();

        Profesional guardado = profesionalRepository.save(profesional);
        log.info("Profesional registrado con id={}: {} {}", guardado.getId(), guardado.getNombre(), guardado.getApellido());
        return mapToDTO(guardado);
    }

    @Transactional
    public ProfesionalResponseDTO actualizarServiciosHabilitados(Long profesionalId, List<Long> serviciosIds) {
        Profesional profesional = buscarEntidadPorId(profesionalId);
        List<Servicio> servicios = servicioService.buscarEntidadesPorIds(serviciosIds);

        validarCoherenciaSede(profesional.getConsultorioFijo(), new HashSet<>(servicios));

        profesional.getServiciosHabilitados().clear();
        profesional.getServiciosHabilitados().addAll(servicios);

        Profesional actualizado = profesionalRepository.save(profesional);
        log.info("Servicios actualizados para profesional id={}: {} servicios habilitados",
                profesionalId, servicios.size());
        return mapToDTO(actualizado);
    }

    @Transactional(readOnly = true)
    public List<ProfesionalResponseDTO> listarProfesionales(Long especialidadId, Long servicioId, Boolean activo) {
        List<Profesional> profesionales = profesionalRepository.searchByFiltros(especialidadId, servicioId, activo);
        return profesionales.stream().map(this::mapToDTO).toList();
    }

    @Transactional(readOnly = true)
    public Profesional buscarEntidadPorId(Long id) {
        return profesionalRepository.findWithDetailsById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el profesional con id: " + id));
    }

    // =========================================================================
    // PLANTILLA HORARIA Y BLOQUEOS PUNTUALES DE AGENDA (RN-01, RN-06, FUNC-10)
    // =========================================================================

    @Transactional
    public HorarioProfesionalResponseDTO registrarHorario(Long profesionalId, HorarioProfesionalRequestDTO request) {
        Profesional profesional = buscarEntidadPorId(profesionalId);

        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la hora de inicio");
        }

        List<HorarioProfesional> horariosDia = horarioProfesionalRepository
                .findByProfesionalIdAndDiaSemanaAndActivoTrue(profesionalId, request.getDiaSemana());

        for (HorarioProfesional h : horariosDia) {
            if (request.getHoraInicio().isBefore(h.getHoraFin()) && request.getHoraFin().isAfter(h.getHoraInicio())) {
                throw new ReglaNegocioException("Existe superposición horaria con otro bloque configurado para el "
                        + request.getDiaSemana() + " (" + h.getHoraInicio() + " - " + h.getHoraFin() + ")");
            }
        }

        HorarioProfesional horario = HorarioProfesional.builder()
                .profesional(profesional)
                .diaSemana(request.getDiaSemana())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .build();

        HorarioProfesional guardado = horarioProfesionalRepository.save(horario);
        log.info("Horario registrado para profesional id={}: {} {} - {}",
                profesionalId, guardado.getDiaSemana(), guardado.getHoraInicio(), guardado.getHoraFin());

        return mapHorarioToDTO(guardado);
    }

    @Transactional(readOnly = true)
    public AgendaProfesionalResponseDTO obtenerAgenda(Long profesionalId) {
        Profesional profesional = buscarEntidadPorId(profesionalId);

        List<HorarioProfesionalResponseDTO> horarios = horarioProfesionalRepository
                .findByProfesionalId(profesionalId).stream()
                .map(this::mapHorarioToDTO)
                .toList();

        List<BloqueoAgendaResponseDTO> bloqueos = bloqueoProfesionalRepository
                .findByProfesionalId(profesionalId).stream()
                .map(this::mapBloqueoToDTO)
                .toList();

        return AgendaProfesionalResponseDTO.builder()
                .profesionalId(profesional.getId())
                .profesionalNombreCompleto(profesional.getNombre() + " " + profesional.getApellido())
                .horariosSemanales(horarios)
                .bloqueos(bloqueos)
                .build();
    }

    @Transactional
    public BloqueoAgendaResponseDTO registrarBloqueo(Long profesionalId, BloqueoAgendaRequestDTO request) {
        Profesional profesional = buscarEntidadPorId(profesionalId);
        if (Boolean.FALSE.equals(profesional.getActivo())) {
            throw new ReglaNegocioException("No se puede registrar un bloqueo para un profesional inactivo");
        }

        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new ReglaNegocioException("La hora de fin del bloqueo debe ser posterior a la hora de inicio");
        }

        BloqueoProfesional bloqueo = BloqueoProfesional.builder()
                .profesional(profesional)
                .fecha(request.getFecha())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .motivo(request.getMotivo() != null ? request.getMotivo().trim() : "Bloqueo de agenda médica")
                .build();

        BloqueoProfesional guardado = bloqueoProfesionalRepository.save(bloqueo);
        log.info("Bloqueo registrado para profesional id={} fecha={} franja=[{}, {}]: {}",
                profesionalId, guardado.getFecha(), guardado.getHoraInicio(), guardado.getHoraFin(), guardado.getMotivo());

        return mapBloqueoToDTO(guardado);
    }

    // =========================================================================
    // VALIDACIONES DE COHERENCIA Y MAPEOS
    // =========================================================================

    private void validarCoherenciaSede(Consultorio consultorio, Set<Servicio> servicios) {
        Long sedeConsultorioId = consultorio.getSede().getId();
        for (Servicio servicio : servicios) {
            for (Equipamiento equipamiento : servicio.getEquipamientosRequeridos()) {
                if (!sedeConsultorioId.equals(equipamiento.getSede().getId())) {
                    throw new CoherenciaSedeException("Incoherencia física de sede: el consultorio fijo pertenece a la sede id="
                            + sedeConsultorioId + ", pero el equipamiento '" + equipamiento.getNombre()
                            + "' requerido por el servicio '" + servicio.getNombre()
                            + "' pertenece a la sede id=" + equipamiento.getSede().getId());
                }
            }
        }
    }

    public ProfesionalResponseDTO mapToDTO(Profesional profesional) {
        return ProfesionalResponseDTO.builder()
                .id(profesional.getId())
                .keycloakSub(profesional.getKeycloakSub())
                .especialidadId(profesional.getEspecialidad().getId())
                .especialidadNombre(profesional.getEspecialidad().getNombre())
                .consultorioFijoId(profesional.getConsultorioFijo().getId())
                .consultorioCodigo(profesional.getConsultorioFijo().getCodigoNombre())
                .sedeId(profesional.getConsultorioFijo().getSede().getId())
                .sedeNombre(profesional.getConsultorioFijo().getSede().getNombre())
                .matricula(profesional.getMatricula())
                .nombre(profesional.getNombre())
                .apellido(profesional.getApellido())
                .email(profesional.getEmail())
                .activo(profesional.getActivo())
                .serviciosHabilitadosIds(
                        profesional.getServiciosHabilitados().stream()
                                .map(Servicio::getId)
                                .toList()
                )
                .build();
    }

    public HorarioProfesionalResponseDTO mapHorarioToDTO(HorarioProfesional h) {
        return HorarioProfesionalResponseDTO.builder()
                .id(h.getId())
                .profesionalId(h.getProfesional().getId())
                .diaSemana(h.getDiaSemana())
                .horaInicio(h.getHoraInicio())
                .horaFin(h.getHoraFin())
                .activo(h.getActivo())
                .build();
    }

    public BloqueoAgendaResponseDTO mapBloqueoToDTO(BloqueoProfesional b) {
        return BloqueoAgendaResponseDTO.builder()
                .id(b.getId())
                .profesionalId(b.getProfesional().getId())
                .fecha(b.getFecha())
                .horaInicio(b.getHoraInicio())
                .horaFin(b.getHoraFin())
                .motivo(b.getMotivo())
                .build();
    }
}
