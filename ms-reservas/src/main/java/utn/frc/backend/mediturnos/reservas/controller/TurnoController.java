package utn.frc.backend.mediturnos.reservas.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import utn.frc.backend.mediturnos.common.enums.EstadoTurno;
import utn.frc.backend.mediturnos.reservas.dto.*;
import utn.frc.backend.mediturnos.reservas.service.TurnoService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    @PostMapping
    public ResponseEntity<TurnoResponseDTO> solicitarTurno(@Valid @RequestBody TurnoRequestDTO request) {
        TurnoResponseDTO creado = turnoService.solicitarTurno(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TurnoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(turnoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<TurnoResponseDTO>> listarTurnos(
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Long profesionalId,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) EstadoTurno estado) {
        return ResponseEntity.ok(turnoService.listarTurnos(pacienteId, profesionalId, sedeId, fecha, estado));
    }

    @PostMapping("/{id}/pagos")
    public ResponseEntity<PagoResponseDTO> registrarPago(
            @PathVariable Long id,
            @Valid @RequestBody PagoRequestDTO request) {
        PagoResponseDTO pago = turnoService.registrarPago(id, request);
        return ResponseEntity.ok(pago);
    }

    @PatchMapping("/{id}/cancelacion")
    public ResponseEntity<TurnoResponseDTO> cancelarTurnoPaciente(@PathVariable Long id) {
        return ResponseEntity.ok(turnoService.cancelarTurnoPaciente(id));
    }

    @PostMapping("/cancelacion-bloque-profesional")
    public ResponseEntity<CancelacionBloqueProfesionalResponseDTO> cancelarBloqueProfesional(
            @Valid @RequestBody CancelacionBloqueProfesionalRequestDTO request) {
        CancelacionBloqueProfesionalResponseDTO response = turnoService.cancelarBloqueProfesional(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reprogramacion")
    public ResponseEntity<TurnoResponseDTO> reprogramarTurno(
            @PathVariable Long id,
            @Valid @RequestBody ReprogramacionTurnoRequestDTO request) {
        return ResponseEntity.ok(turnoService.reprogramarTurno(id, request));
    }

    @PatchMapping("/{id}/cierre-atencion")
    public ResponseEntity<TurnoResponseDTO> cerrarAtencion(
            @PathVariable Long id,
            @Valid @RequestBody CierreAtencionRequestDTO request) {
        return ResponseEntity.ok(turnoService.cerrarAtencion(id, request));
    }
}
