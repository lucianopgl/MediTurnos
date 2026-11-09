package utn.frc.backend.mediturnos.catalogo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.frc.backend.mediturnos.catalogo.dto.ConsultorioRequestDTO;
import utn.frc.backend.mediturnos.catalogo.dto.ConsultorioResponseDTO;
import utn.frc.backend.mediturnos.catalogo.entity.Consultorio;
import utn.frc.backend.mediturnos.catalogo.entity.Sede;
import utn.frc.backend.mediturnos.catalogo.repository.ConsultorioRepository;
import utn.frc.backend.mediturnos.common.exception.RecursoNoDisponibleException;
import utn.frc.backend.mediturnos.common.exception.RecursoNoEncontradoException;
import utn.frc.backend.mediturnos.common.exception.ReglaNegocioException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultorioService {

    private final ConsultorioRepository consultorioRepository;
    private final SedeService sedeService;

    @Transactional
    public ConsultorioResponseDTO registrarConsultorio(ConsultorioRequestDTO request) {
        Sede sede = sedeService.buscarEntidadPorId(request.getSedeId());
        if (Boolean.FALSE.equals(sede.getActiva())) {
            throw new RecursoNoDisponibleException("No se puede registrar un consultorio en una sede inactiva");
        }

        if (consultorioRepository.existsBySedeIdAndCodigoNombreIgnoreCase(request.getSedeId(), request.getCodigoNombre())) {
            throw new ReglaNegocioException("Ya existe un consultorio con el código/nombre '"
                    + request.getCodigoNombre() + "' en la sede " + sede.getNombre());
        }

        Consultorio consultorio = Consultorio.builder()
                .sede(sede)
                .codigoNombre(request.getCodigoNombre().trim())
                .operativo(request.getOperativo() != null ? request.getOperativo() : true)
                .build();

        Consultorio guardado = consultorioRepository.save(consultorio);
        log.info("Consultorio registrado con id={} en sede={}", guardado.getId(), sede.getId());
        return mapToDTO(guardado);
    }

    @Transactional
    public ConsultorioResponseDTO actualizarEstadoOperativo(Long id, Boolean operativo) {
        Consultorio consultorio = buscarEntidadPorId(id);
        consultorio.setOperativo(operativo);
        Consultorio actualizado = consultorioRepository.save(consultorio);
        log.info("Consultorio id={} actualizó operativo={}", id, operativo);
        return mapToDTO(actualizado);
    }

    @Transactional(readOnly = true)
    public List<ConsultorioResponseDTO> listarConsultorios(Long sedeId, Boolean operativo) {
        List<Consultorio> consultorios;
        if (sedeId != null && operativo != null) {
            consultorios = consultorioRepository.findBySedeIdAndOperativo(sedeId, operativo);
        } else if (sedeId != null) {
            consultorios = consultorioRepository.findBySedeId(sedeId);
        } else if (operativo != null) {
            consultorios = consultorioRepository.findByOperativo(operativo);
        } else {
            consultorios = consultorioRepository.findAll();
        }
        return consultorios.stream().map(this::mapToDTO).toList();
    }

    @Transactional(readOnly = true)
    public Consultorio buscarEntidadPorId(Long id) {
        return consultorioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el consultorio con id: " + id));
    }

    public ConsultorioResponseDTO mapToDTO(Consultorio consultorio) {
        return ConsultorioResponseDTO.builder()
                .id(consultorio.getId())
                .sedeId(consultorio.getSede().getId())
                .sedeNombre(consultorio.getSede().getNombre())
                .codigoNombre(consultorio.getCodigoNombre())
                .operativo(consultorio.getOperativo())
                .build();
    }
}
