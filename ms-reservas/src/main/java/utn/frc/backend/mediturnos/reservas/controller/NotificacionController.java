package utn.frc.backend.mediturnos.reservas.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import utn.frc.backend.mediturnos.common.enums.EstadoEnvioNotificacion;
import utn.frc.backend.mediturnos.reservas.dto.NotificacionResponseDTO;
import utn.frc.backend.mediturnos.reservas.service.NotificacionService;

import java.util.List;

@RestController
@RequestMapping("/api/reservas/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<List<NotificacionResponseDTO>> listarNotificaciones(
            @RequestParam(required = false) EstadoEnvioNotificacion estado,
            @RequestParam(required = false) Long pacienteId) {
        return ResponseEntity.ok(notificacionService.listarNotificaciones(estado, pacienteId));
    }
}
