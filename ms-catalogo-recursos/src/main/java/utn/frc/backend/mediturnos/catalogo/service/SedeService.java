package utn.frc.backend.mediturnos.catalogo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.frc.backend.mediturnos.catalogo.dto.SedeRequestDTO;
import utn.frc.backend.mediturnos.catalogo.dto.SedeResponseDTO;
import utn.frc.backend.mediturnos.catalogo.entity.Sede;
import utn.frc.backend.mediturnos.catalogo.repository.SedeRepository;
import utn.frc.backend.mediturnos.common.exception.RecursoNoEncontradoException;
import utn.frc.backend.mediturnos.common.exception.ReglaNegocioException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SedeService {

    private final SedeRepository sedeRepository;

    @Transactional
    public SedeResponseDTO registrarSede(SedeRequestDTO request) {
        if (sedeRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new ReglaNegocioException("Ya existe una sede registrada con el nombre: " + request.getNombre());
        }

        Sede sede = Sede.builder()
                .nombre(request.getNombre().trim())
                .direccion(request.getDireccion().trim())
                .telefono(request.getTelefono() != null ? request.getTelefono().trim() : null)
                .activa(request.getActiva() != null ? request.getActiva() : true)
                .build();

        Sede guardada = sedeRepository.save(sede);
        log.info("Sede registrada con id={}: {}", guardada.getId(), guardada.getNombre());
        return mapToDTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<SedeResponseDTO> listarSedes(Boolean activa) {
        List<Sede> sedes = activa != null
                ? sedeRepository.findByActiva(activa)
                : sedeRepository.findAll();
        return sedes.stream().map(this::mapToDTO).toList();
    }

    @Transactional(readOnly = true)
    public Sede buscarEntidadPorId(Long id) {
        return sedeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la sede con id: " + id));
    }

    public SedeResponseDTO mapToDTO(Sede sede) {
        return SedeResponseDTO.builder()
                .id(sede.getId())
                .nombre(sede.getNombre())
                .direccion(sede.getDireccion())
                .telefono(sede.getTelefono())
                .activa(sede.getActiva())
                .build();
    }
}
