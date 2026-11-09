package utn.frc.backend.mediturnos.catalogo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import utn.frc.backend.mediturnos.catalogo.dto.*;
import utn.frc.backend.mediturnos.catalogo.service.ProfesionalService;
import utn.frc.backend.mediturnos.common.dto.BloqueoAgendaRequestDTO;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/profesionales")
@RequiredArgsConstructor
public class ProfesionalController {

    private final ProfesionalService profesionalService;

    @PostMapping
    public ResponseEntity<ProfesionalResponseDTO> registrarProfesional(@Valid @RequestBody ProfesionalRequestDTO request) {
        ProfesionalResponseDTO creado = profesionalService.registrarProfesional(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    public ResponseEntity<List<ProfesionalResponseDTO>> listarProfesionales(
            @RequestParam(required = false) Long especialidadId,
            @RequestParam(required = false) Long servicioId,
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(profesionalService.listarProfesionales(especialidadId, servicioId, activo));
    }

    @PutMapping("/{id}/servicios")
    public ResponseEntity<ProfesionalResponseDTO> actualizarServiciosHabilitados(
            @PathVariable Long id,
            @Valid @RequestBody ProfesionalServiciosRequestDTO request) {
        ProfesionalResponseDTO actualizado = profesionalService.actualizarServiciosHabilitados(id, request.getServiciosIds());
        return ResponseEntity.ok(actualizado);
    }

    @PostMapping("/{id}/horarios")
    public ResponseEntity<HorarioProfesionalResponseDTO> registrarHorario(
            @PathVariable Long id,
            @Valid @RequestBody HorarioProfesionalRequestDTO request) {
        HorarioProfesionalResponseDTO horario = profesionalService.registrarHorario(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(horario);
    }

    @GetMapping("/{id}/horarios")
    public ResponseEntity<AgendaProfesionalResponseDTO> obtenerAgenda(@PathVariable Long id) {
        return ResponseEntity.ok(profesionalService.obtenerAgenda(id));
    }

    @PostMapping("/{id}/bloqueos")
    public ResponseEntity<BloqueoAgendaResponseDTO> registrarBloqueo(
            @PathVariable Long id,
            @Valid @RequestBody BloqueoAgendaRequestDTO request) {
        BloqueoAgendaResponseDTO bloqueo = profesionalService.registrarBloqueo(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(bloqueo);
    }
}
