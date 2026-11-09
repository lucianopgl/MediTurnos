package utn.frc.backend.mediturnos.catalogo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import utn.frc.backend.mediturnos.catalogo.config.CustomSecurityExceptionHandler;
import utn.frc.backend.mediturnos.catalogo.config.SecurityConfig;
import utn.frc.backend.mediturnos.catalogo.dto.EstadoOperativoRequestDTO;
import utn.frc.backend.mediturnos.catalogo.dto.SedeRequestDTO;
import utn.frc.backend.mediturnos.catalogo.dto.SedeResponseDTO;
import utn.frc.backend.mediturnos.catalogo.service.*;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ActiveProfiles("test")
@Import({SecurityConfig.class, CustomSecurityExceptionHandler.class})
class CatalogoControllersSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SedeService sedeService;

    @MockBean
    private ConsultorioService consultorioService;

    @MockBean
    private EquipamientoService equipamientoService;

    @MockBean
    private EspecialidadService especialidadService;

    @MockBean
    private ServicioService servicioService;

    @MockBean
    private ProfesionalService profesionalService;

    @MockBean
    private CompatibilidadService compatibilidadService;

    @Test
    @DisplayName("CV-08: Petición sin token JWT a GET /api/catalogo/sedes retorna 401 Unauthorized")
    void testGetSedes_SinToken_Retorna401() throws Exception {
        mockMvc.perform(get("/api/catalogo/sedes")
                        .header("X-Correlation-Id", "test-corr-001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.ruleCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.correlationId").value("test-corr-001"));
    }

    @Test
    @DisplayName("CV-08: Petición con ROL_PACIENTE a endpoint exclusivo de admin retorna 403 Forbidden")
    void testPostSede_ConRolPaciente_Retorna403() throws Exception {
        SedeRequestDTO request = SedeRequestDTO.builder()
                .nombre("Nueva Sede")
                .direccion("Calle Falsa 123")
                .activa(true)
                .build();

        mockMvc.perform(post("/api/catalogo/sedes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROL_PACIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Correlation-Id", "test-corr-002"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.ruleCode").value("FORBIDDEN"))
                .andExpect(jsonPath("$.correlationId").value("test-corr-002"));
    }

    @Test
    @DisplayName("CV-08: Petición con ROL_PACIENTE a PATCH estado-operativo retorna 403 Forbidden")
    void testPatchEquipamiento_ConRolPaciente_Retorna403() throws Exception {
        EstadoOperativoRequestDTO request = new EstadoOperativoRequestDTO(false);

        mockMvc.perform(patch("/api/catalogo/equipamientos/1/estado-operativo")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROL_PACIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CV-08: Petición con ROL_ADMIN a POST /api/catalogo/sedes retorna 201 Created")
    void testPostSede_ConRolAdmin_Retorna201() throws Exception {
        SedeRequestDTO request = SedeRequestDTO.builder()
                .nombre("Sede Norte")
                .direccion("Av. Juan B. Justo 5000")
                .activa(true)
                .build();

        SedeResponseDTO response = SedeResponseDTO.builder()
                .id(3L)
                .nombre("Sede Norte")
                .direccion("Av. Juan B. Justo 5000")
                .activa(true)
                .build();

        when(sedeService.registrarSede(any())).thenReturn(response);

        mockMvc.perform(post("/api/catalogo/sedes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROL_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.nombre").value("Sede Norte"));
    }

    @Test
    @DisplayName("GET /api/catalogo/sedes con ROL_PACIENTE retorna 200 OK")
    void testGetSedes_ConRolPaciente_Retorna200() throws Exception {
        when(sedeService.listarSedes(null)).thenReturn(List.of(
                SedeResponseDTO.builder().id(1L).nombre("Sede Centro").build()
        ));

        mockMvc.perform(get("/api/catalogo/sedes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROL_PACIENTE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Sede Centro"));
    }
}
