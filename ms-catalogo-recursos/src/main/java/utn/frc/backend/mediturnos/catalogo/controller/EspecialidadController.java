package utn.frc.backend.mediturnos.catalogo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import utn.frc.backend.mediturnos.catalogo.dto.EspecialidadRequestDTO;
import utn.frc.backend.mediturnos.catalogo.dto.EspecialidadResponseDTO;
import utn.frc.backend.mediturnos.catalogo.service.EspecialidadService;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @PostMapping
    public ResponseEntity<EspecialidadResponseDTO> registrarEspecialidad(@Valid @RequestBody EspecialidadRequestDTO request) {
        EspecialidadResponseDTO creada = especialidadService.registrarEspecialidad(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping
    public ResponseEntity<List<EspecialidadResponseDTO>> listarEspecialidades(@RequestParam(required = false) Boolean activa) {
        return ResponseEntity.ok(especialidadService.listarEspecialidades(activa));
    }
}
