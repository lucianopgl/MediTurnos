package utn.frc.backend.mediturnos.catalogo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.frc.backend.mediturnos.catalogo.dto.ServicioRequestDTO;
import utn.frc.backend.mediturnos.catalogo.dto.ServicioResponseDTO;
import utn.frc.backend.mediturnos.catalogo.entity.Equipamiento;
import utn.frc.backend.mediturnos.catalogo.entity.Especialidad;
import utn.frc.backend.mediturnos.catalogo.entity.Servicio;
import utn.frc.backend.mediturnos.catalogo.repository.ServicioRepository;
import utn.frc.backend.mediturnos.common.exception.RecursoNoDisponibleException;
import utn.frc.backend.mediturnos.common.exception.RecursoNoEncontradoException;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final EspecialidadService especialidadService;
    private final EquipamientoService equipamientoService;

    @Transactional
    public ServicioResponseDTO registrarServicio(ServicioRequestDTO request) {
        Especialidad especialidad = especialidadService.buscarEntidadPorId(request.getEspecialidadId());
        if (Boolean.FALSE.equals(especialidad.getActiva())) {
            throw new RecursoNoDisponibleException("No se puede registrar un servicio para una especialidad inactiva");
        }

        Set<Equipamiento> equipamientos = new HashSet<>();
        if (request.getEquipamientosIds() != null && !request.getEquipamientosIds().isEmpty()) {
            equipamientos.addAll(equipamientoService.buscarEntidadesPorIds(request.getEquipamientosIds()));
        }

        Servicio servicio = Servicio.builder()
                .especialidad(especialidad)
                .nombre(request.getNombre().trim())
                .descripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null)
                .duracionMinutos(request.getDuracionMinutos())
                .costo(request.getCosto())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .equipamientosRequeridos(equipamientos)
                .build();

        Servicio guardado = servicioRepository.save(servicio);
        log.info("Servicio registrado con id={}: {}", guardado.getId(), guardado.getNombre());
        return mapToDTO(guardado);
    }

    @Transactional(readOnly = true)
    public List<ServicioResponseDTO> listarServicios(Long especialidadId, Boolean activo) {
        List<Servicio> servicios = servicioRepository.searchByFiltros(especialidadId, activo);
        return servicios.stream().map(this::mapToDTO).toList();
    }

    @Transactional(readOnly = true)
    public Servicio buscarEntidadPorId(Long id) {
        return servicioRepository.findWithDetailsById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el servicio con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Servicio> buscarEntidadesPorIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Servicio> encontrados = servicioRepository.findAllByIdIn(ids);
        if (encontrados.size() != ids.size()) {
            throw new RecursoNoEncontradoException("Uno o más identificadores de servicio no existen en el catálogo");
        }
        return encontrados;
    }

    public ServicioResponseDTO mapToDTO(Servicio servicio) {
        return ServicioResponseDTO.builder()
                .id(servicio.getId())
                .especialidadId(servicio.getEspecialidad().getId())
                .especialidadNombre(servicio.getEspecialidad().getNombre())
                .nombre(servicio.getNombre())
                .descripcion(servicio.getDescripcion())
                .duracionMinutos(servicio.getDuracionMinutos())
                .costo(servicio.getCosto())
                .activo(servicio.getActivo())
                .equipamientosRequeridos(
                        servicio.getEquipamientosRequeridos().stream()
                                .map(equipamientoService::mapToDTO)
                                .toList()
                )
                .build();
    }
}
