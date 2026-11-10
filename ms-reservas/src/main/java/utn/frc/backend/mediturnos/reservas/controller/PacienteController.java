package utn.frc.backend.mediturnos.reservas.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import utn.frc.backend.mediturnos.reservas.dto.PacienteRequestDTO;
import utn.frc.backend.mediturnos.reservas.dto.PacienteResponseDTO;
import utn.frc.backend.mediturnos.reservas.service.PacienteService;

@RestController
@RequestMapping("/api/reservas/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    @PostMapping
    public ResponseEntity<PacienteResponseDTO> registrarPaciente(@Valid @RequestBody PacienteRequestDTO request) {
        PacienteResponseDTO creado = pacienteService.registrarPaciente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/mi-perfil")
    public ResponseEntity<PacienteResponseDTO> obtenerMiPerfil() {
        return ResponseEntity.ok(pacienteService.obtenerMiPerfil());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.obtenerPorId(id));
    }
}
