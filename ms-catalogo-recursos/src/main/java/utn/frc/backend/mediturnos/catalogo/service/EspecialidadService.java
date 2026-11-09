package utn.frc.backend.mediturnos.catalogo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.frc.backend.mediturnos.catalogo.dto.EspecialidadRequestDTO;
import utn.frc.backend.mediturnos.catalogo.dto.EspecialidadResponseDTO;
import utn.frc.backend.mediturnos.catalogo.entity.Especialidad;
import utn.frc.backend.mediturnos.catalogo.repository.EspecialidadRepository;
import utn.frc.backend.mediturnos.common.exception.RecursoNoEncontradoException;
import utn.frc.backend.mediturnos.common.exception.ReglaNegocioException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    @Transactional
    public EspecialidadResponseDTO registrarEspecialidad(EspecialidadRequestDTO request) {
        if (especialidadRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new ReglaNegocioException("Ya existe una especialidad registrada con el nombre: " + request.getNombre());
        }

        Especialidad especialidad = Especialidad.builder()
                .nombre(request.getNombre().trim())
                .descripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null)
                .activa(request.getActiva() != null ? request.getActiva() : true)
                .build();

        Especialidad guardada = especialidadRepository.save(especialidad);
        log.info("Especialidad registrada con id={}: {}", guardada.getId(), guardada.getNombre());
        return mapToDTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<EspecialidadResponseDTO> listarEspecialidades(Boolean activa) {
        List<Especialidad> lista = activa != null
                ? especialidadRepository.findByActiva(activa)
                : especialidadRepository.findAll();
        return lista.stream().map(this::mapToDTO).toList();
    }

    @Transactional(readOnly = true)
    public Especialidad buscarEntidadPorId(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la especialidad con id: " + id));
    }

    public EspecialidadResponseDTO mapToDTO(Especialidad especialidad) {
        return EspecialidadResponseDTO.builder()
                .id(especialidad.getId())
                .nombre(especialidad.getNombre())
                .descripcion(especialidad.getDescripcion())
                .activa(especialidad.getActiva())
                .build();
    }
}
