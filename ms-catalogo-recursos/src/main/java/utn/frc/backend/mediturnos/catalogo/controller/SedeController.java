package utn.frc.backend.mediturnos.catalogo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import utn.frc.backend.mediturnos.catalogo.dto.SedeRequestDTO;
import utn.frc.backend.mediturnos.catalogo.dto.SedeResponseDTO;
import utn.frc.backend.mediturnos.catalogo.service.SedeService;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/sedes")
@RequiredArgsConstructor
public class SedeController {

    private final SedeService sedeService;

    @PostMapping
    public ResponseEntity<SedeResponseDTO> registrarSede(@Valid @RequestBody SedeRequestDTO request) {
        SedeResponseDTO creada = sedeService.registrarSede(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping
    public ResponseEntity<List<SedeResponseDTO>> listarSedes(@RequestParam(required = false) Boolean activa) {
        return ResponseEntity.ok(sedeService.listarSedes(activa));
    }
}
