package utn.frc.backend.mediturnos.reservas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.frc.backend.mediturnos.common.exception.RecursoNoDisponibleException;
import utn.frc.backend.mediturnos.common.exception.RecursoNoEncontradoException;
import utn.frc.backend.mediturnos.reservas.dto.PacienteRequestDTO;
import utn.frc.backend.mediturnos.reservas.dto.PacienteResponseDTO;
import utn.frc.backend.mediturnos.reservas.entity.Paciente;
import utn.frc.backend.mediturnos.reservas.repository.PacienteRepository;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    @Transactional
    public PacienteResponseDTO registrarPaciente(PacienteRequestDTO request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROL_ADMIN"));

        String keycloakSub = (esAdmin && request.getKeycloakSub() != null && !request.getKeycloakSub().isBlank())
                ? request.getKeycloakSub().trim()
                : obtenerSubjectAutenticado();

        String dniNorm = request.getDni().trim();
        String emailNorm = request.getEmail().trim().toLowerCase();

        if (pacienteRepository.existsByKeycloakSub(keycloakSub)) {
            throw new RecursoNoDisponibleException("Ya existe un perfil de paciente registrado para este usuario de Keycloak");
        }
        if (pacienteRepository.existsByDni(dniNorm)) {
            throw new RecursoNoDisponibleException("El DNI ingresado ya se encuentra registrado en el sistema");
        }
        if (pacienteRepository.existsByEmail(emailNorm)) {
            throw new RecursoNoDisponibleException("El email ingresado ya se encuentra registrado en el sistema");
        }

        Paciente paciente = Paciente.builder()
                .keycloakSub(keycloakSub)
                .dni(dniNorm)
                .nombre(request.getNombre().trim())
                .apellido(request.getApellido().trim())
                .email(emailNorm)
                .telefonoWhatsapp(request.getTelefonoWhatsapp().trim())
                .cantidadPenalizaciones(0)
                .activo(true)
                .build();

        Paciente guardado = pacienteRepository.save(paciente);
        return mapToDTO(guardado);
    }

    @Transactional(readOnly = true)
    public PacienteResponseDTO obtenerMiPerfil() {
        String keycloakSub = obtenerSubjectAutenticado();
        Paciente paciente = pacienteRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el perfil del paciente autenticado. Debe registrarse primero."));
        return mapToDTO(paciente);
    }

    @Transactional(readOnly = true)
    public PacienteResponseDTO obtenerPorId(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado con id: " + id));
        return mapToDTO(paciente);
    }

    @Transactional(readOnly = true)
    public Paciente obtenerEntidadPorId(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado con id: " + id));
        if (Boolean.FALSE.equals(paciente.getActivo())) {
            throw new RecursoNoDisponibleException("El perfil del paciente con id " + id + " se encuentra inactivo");
        }
        return paciente;
    }

    @Transactional(readOnly = true)
    public Paciente obtenerEntidadAutenticada() {
        String keycloakSub = obtenerSubjectAutenticado();
        Paciente paciente = pacienteRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el perfil del paciente autenticado con sub: " + keycloakSub));
        if (Boolean.FALSE.equals(paciente.getActivo())) {
            throw new RecursoNoDisponibleException("El perfil del paciente autenticado se encuentra inactivo");
        }
        return paciente;
    }

    private String obtenerSubjectAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        throw new RecursoNoEncontradoException("No se encontró contexto de autenticación JWT válido");
    }

    private PacienteResponseDTO mapToDTO(Paciente p) {
        return PacienteResponseDTO.builder()
                .id(p.getId())
                .keycloakSub(p.getKeycloakSub())
                .dni(p.getDni())
                .nombre(p.getNombre())
                .apellido(p.getApellido())
                .email(p.getEmail())
                .telefonoWhatsapp(p.getTelefonoWhatsapp())
                .cantidadPenalizaciones(p.getCantidadPenalizaciones())
                .activo(p.getActivo())
                .fechaAlta(p.getFechaAlta())
                .build();
    }
}
