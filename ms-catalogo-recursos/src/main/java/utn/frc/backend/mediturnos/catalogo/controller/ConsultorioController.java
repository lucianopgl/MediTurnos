package utn.frc.backend.mediturnos.catalogo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import utn.frc.backend.mediturnos.catalogo.dto.ConsultorioRequestDTO;
import utn.frc.backend.mediturnos.catalogo.dto.ConsultorioResponseDTO;
import utn.frc.backend.mediturnos.catalogo.dto.EstadoOperativoRequestDTO;
import utn.frc.backend.mediturnos.catalogo.service.ConsultorioService;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/consultorios")
@RequiredArgsConstructor
public class ConsultorioController {

    private final ConsultorioService consultorioService;

    @PostMapping
    public ResponseEntity<ConsultorioResponseDTO> registrarConsultorio(@Valid @RequestBody ConsultorioRequestDTO request) {
        ConsultorioResponseDTO creado = consultorioService.registrarConsultorio(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PatchMapping("/{id}/estado-operativo")
    public ResponseEntity<ConsultorioResponseDTO> actualizarEstadoOperativo(
            @PathVariable Long id,
            @Valid @RequestBody EstadoOperativoRequestDTO request) {
        ConsultorioResponseDTO actualizado = consultorioService.actualizarEstadoOperativo(id, request.getOperativo());
        return ResponseEntity.ok(actualizado);
    }

    @GetMapping
    public ResponseEntity<List<ConsultorioResponseDTO>> listarConsultorios(
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Boolean operativo) {
        return ResponseEntity.ok(consultorioService.listarConsultorios(sedeId, operativo));
    }
}
