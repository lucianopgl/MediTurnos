package utn.frc.backend.mediturnos.catalogo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.frc.backend.mediturnos.catalogo.dto.EquipamientoRequestDTO;
import utn.frc.backend.mediturnos.catalogo.dto.EquipamientoResponseDTO;
import utn.frc.backend.mediturnos.catalogo.entity.Equipamiento;
import utn.frc.backend.mediturnos.catalogo.entity.Sede;
import utn.frc.backend.mediturnos.catalogo.repository.EquipamientoRepository;
import utn.frc.backend.mediturnos.common.exception.RecursoNoDisponibleException;
import utn.frc.backend.mediturnos.common.exception.RecursoNoEncontradoException;
import utn.frc.backend.mediturnos.common.exception.ReglaNegocioException;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipamientoService {

    private final EquipamientoRepository equipamientoRepository;
    private final SedeService sedeService;

    @Transactional
    public EquipamientoResponseDTO registrarEquipamiento(EquipamientoRequestDTO request) {
        Sede sede = sedeService.buscarEntidadPorId(request.getSedeId());
        if (Boolean.FALSE.equals(sede.getActiva())) {
            throw new RecursoNoDisponibleException("No se puede registrar equipamiento en una sede inactiva");
        }

        if (equipamientoRepository.existsByCodigoInventarioIgnoreCase(request.getCodigoInventario())) {
            throw new ReglaNegocioException("Ya existe un equipamiento registrado con el código de inventario: "
                    + request.getCodigoInventario());
        }

        Equipamiento equipamiento = Equipamiento.builder()
                .sede(sede)
                .nombre(request.getNombre().trim())
                .codigoInventario(request.getCodigoInventario().trim())
                .operativo(request.getOperativo() != null ? request.getOperativo() : true)
                .build();

        Equipamiento guardado = equipamientoRepository.save(equipamiento);
        log.info("Equipamiento registrado con id={} en sede={}", guardado.getId(), sede.getId());
        return mapToDTO(guardado);
    }

    @Transactional
    public EquipamientoResponseDTO actualizarEstadoOperativo(Long id, Boolean operativo) {
        Equipamiento equipamiento = buscarEntidadPorId(id);
        equipamiento.setOperativo(operativo);
        Equipamiento actualizado = equipamientoRepository.save(equipamiento);
        log.info("Equipamiento id={} actualizó operativo={}", id, operativo);
        return mapToDTO(actualizado);
    }

    @Transactional(readOnly = true)
    public List<EquipamientoResponseDTO> listarEquipamientos(Long sedeId, Boolean operativo) {
        List<Equipamiento> lista;
        if (sedeId != null && operativo != null) {
            lista = equipamientoRepository.findBySedeIdAndOperativo(sedeId, operativo);
        } else if (sedeId != null) {
            lista = equipamientoRepository.findBySedeId(sedeId);
        } else if (operativo != null) {
            lista = equipamientoRepository.findByOperativo(operativo);
        } else {
            lista = equipamientoRepository.findAll();
        }
        return lista.stream().map(this::mapToDTO).toList();
    }

    @Transactional(readOnly = true)
    public Equipamiento buscarEntidadPorId(Long id) {
        return equipamientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el equipamiento con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Equipamiento> buscarEntidadesPorIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Equipamiento> encontrados = equipamientoRepository.findAllByIdIn(ids);
        if (encontrados.size() != ids.size()) {
            throw new RecursoNoEncontradoException("Uno o más identificadores de equipamiento no existen en el catálogo");
        }
        return encontrados;
    }

    public EquipamientoResponseDTO mapToDTO(Equipamiento equipamiento) {
        return EquipamientoResponseDTO.builder()
                .id(equipamiento.getId())
                .sedeId(equipamiento.getSede().getId())
                .sedeNombre(equipamiento.getSede().getNombre())
                .nombre(equipamiento.getNombre())
                .codigoInventario(equipamiento.getCodigoInventario())
                .operativo(equipamiento.getOperativo())
                .build();
    }
}
