package utn.frc.backend.mediturnos.catalogo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import utn.frc.backend.mediturnos.catalogo.service.CompatibilidadService;
import utn.frc.backend.mediturnos.common.dto.CompatibilidadRecursoDTO;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Endpoint de validación estática y parametrizada de catálogo (FUNC-05 / RN-01 / RN-02).
 * Consumido síncronamente vía RestClient desde ms-reservas y clientes autorizados.
 */
@RestController
@RequestMapping("/api/catalogo/compatibilidad")
@RequiredArgsConstructor
public class CompatibilidadController {

    private final CompatibilidadService compatibilidadService;

    @GetMapping
    public ResponseEntity<CompatibilidadRecursoDTO> evaluarCompatibilidad(
            @RequestParam Long profesionalId,
            @RequestParam Long servicioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio) {

        CompatibilidadRecursoDTO resultado = compatibilidadService.evaluarCompatibilidad(
                profesionalId, servicioId, fecha, horaInicio);
        return ResponseEntity.ok(resultado);
    }
}
