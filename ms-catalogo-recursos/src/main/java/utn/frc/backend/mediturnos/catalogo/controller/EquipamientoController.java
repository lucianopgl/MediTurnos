package utn.frc.backend.mediturnos.catalogo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import utn.frc.backend.mediturnos.catalogo.dto.EquipamientoRequestDTO;
import utn.frc.backend.mediturnos.catalogo.dto.EquipamientoResponseDTO;
import utn.frc.backend.mediturnos.catalogo.dto.EstadoOperativoRequestDTO;
import utn.frc.backend.mediturnos.catalogo.service.EquipamientoService;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/equipamientos")
@RequiredArgsConstructor
public class EquipamientoController {

    private final EquipamientoService equipamientoService;

    @PostMapping
    public ResponseEntity<EquipamientoResponseDTO> registrarEquipamiento(@Valid @RequestBody EquipamientoRequestDTO request) {
        EquipamientoResponseDTO creado = equipamientoService.registrarEquipamiento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PatchMapping("/{id}/estado-operativo")
    public ResponseEntity<EquipamientoResponseDTO> actualizarEstadoOperativo(
            @PathVariable Long id,
            @Valid @RequestBody EstadoOperativoRequestDTO request) {
        EquipamientoResponseDTO actualizado = equipamientoService.actualizarEstadoOperativo(id, request.getOperativo());
        return ResponseEntity.ok(actualizado);
    }

    @GetMapping
    public ResponseEntity<List<EquipamientoResponseDTO>> listarEquipamientos(
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Boolean operativo) {
        return ResponseEntity.ok(equipamientoService.listarEquipamientos(sedeId, operativo));
    }
}
