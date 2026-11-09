package utn.frc.backend.mediturnos.catalogo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import utn.frc.backend.mediturnos.catalogo.dto.ServicioRequestDTO;
import utn.frc.backend.mediturnos.catalogo.dto.ServicioResponseDTO;
import utn.frc.backend.mediturnos.catalogo.service.ServicioService;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService servicioService;

    @PostMapping
    public ResponseEntity<ServicioResponseDTO> registrarServicio(@Valid @RequestBody ServicioRequestDTO request) {
        ServicioResponseDTO creado = servicioService.registrarServicio(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    public ResponseEntity<List<ServicioResponseDTO>> listarServicios(
            @RequestParam(required = false) Long especialidadId,
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(servicioService.listarServicios(especialidadId, activo));
    }
}
