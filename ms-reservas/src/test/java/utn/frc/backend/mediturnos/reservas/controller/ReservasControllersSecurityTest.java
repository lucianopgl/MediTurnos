package utn.frc.backend.mediturnos.reservas.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import utn.frc.backend.mediturnos.reservas.service.NotificacionService;
import utn.frc.backend.mediturnos.reservas.service.PacienteService;
import utn.frc.backend.mediturnos.reservas.service.TurnoService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservasControllersSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PacienteService pacienteService;

    @MockBean
    private TurnoService turnoService;

    @MockBean
    private NotificacionService notificacionService;

    @Test
    @DisplayName("CV-08: Petición sin token JWT debe retornar 401 Unauthorized")
    void testAccesoSinAutenticacionRetorna401() throws Exception {
        mockMvc.perform(get("/api/reservas/notificaciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CV-08: ROL_PACIENTE no tiene permisos para auditar notificaciones (exclusivo ADMIN) -> 403 Forbidden")
    @WithMockUser(username = "paciente", authorities = {"ROL_PACIENTE"})
    void testRolPacienteAccesoNotificacionesRetorna403() throws Exception {
        mockMvc.perform(get("/api/reservas/notificaciones"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CV-08: ROL_PACIENTE no puede bloquear agenda médica (exclusivo PROFESIONAL / ADMIN) -> 403 Forbidden")
    @WithMockUser(username = "paciente", authorities = {"ROL_PACIENTE"})
    void testRolPacienteBloqueoAgendaRetorna403() throws Exception {
        mockMvc.perform(post("/api/reservas/turnos/cancelacion-bloque-profesional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "profesionalId": 1,
                            "fecha": "2026-11-20",
                            "horaInicio": "08:00:00",
                            "horaFin": "12:00:00",
                            "motivo": "Congreso médico"
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CV-08: ROL_ADMIN tiene acceso a la auditoría de notificaciones -> 200 OK")
    @WithMockUser(username = "admin", authorities = {"ROL_ADMIN"})
    void testRolAdminAccesoNotificacionesRetorna200() throws Exception {
        when(notificacionService.listarNotificaciones(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/reservas/notificaciones"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CV-08: Endpoint público /actuator/health accesible sin autenticación -> 200 OK")
    void testActuatorHealthPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
