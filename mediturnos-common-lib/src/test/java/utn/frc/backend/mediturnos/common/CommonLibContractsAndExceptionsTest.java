package utn.frc.backend.mediturnos.common;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import utn.frc.backend.mediturnos.common.dto.CompatibilidadRecursoDTO;
import utn.frc.backend.mediturnos.common.dto.ErrorResponseDTO;
import utn.frc.backend.mediturnos.common.enums.DiaSemana;
import utn.frc.backend.mediturnos.common.enums.EstadoTurno;
import utn.frc.backend.mediturnos.common.exception.CoherenciaSedeException;
import utn.frc.backend.mediturnos.common.exception.ColisionHorariaException;
import utn.frc.backend.mediturnos.common.exception.EstadoTurnoInvalidoException;
import utn.frc.backend.mediturnos.common.exception.GlobalExceptionHandler;
import utn.frc.backend.mediturnos.common.exception.RecursoNoDisponibleException;
import utn.frc.backend.mediturnos.common.exception.RecursoNoEncontradoException;
import utn.frc.backend.mediturnos.common.exception.ReglaNegocioException;
import utn.frc.backend.mediturnos.common.filter.CorrelationIdConstants;
import utn.frc.backend.mediturnos.common.filter.CorrelationIdFilter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonLibContractsAndExceptionsTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("DiaSemana.from(LocalDate) convierte correctamente los 7 días de la semana para RN-01")
    void shouldConvertLocalDateToDiaSemana() {
        // 2026-11-16 es Lunes (usado en los seed data de init-db.sql)
        assertEquals(DiaSemana.LUNES, DiaSemana.from(LocalDate.of(2026, 11, 16)));
        assertEquals(DiaSemana.MARTES, DiaSemana.from(LocalDate.of(2026, 11, 17)));
        assertEquals(DiaSemana.MIERCOLES, DiaSemana.from(LocalDate.of(2026, 11, 18)));
        assertEquals(DiaSemana.JUEVES, DiaSemana.from(LocalDate.of(2026, 11, 19)));
        assertEquals(DiaSemana.VIERNES, DiaSemana.from(LocalDate.of(2026, 11, 20)));
        assertEquals(DiaSemana.SABADO, DiaSemana.from(LocalDate.of(2026, 11, 21)));
        assertEquals(DiaSemana.DOMINGO, DiaSemana.from(LocalDate.of(2026, 11, 22)));
    }

    @Test
    @DisplayName("EstadoTurno.ocupaRecursos() retorna true solo para RESERVADO y CONFIRMADO (RN-03)")
    void shouldIdentifyStatesOccupyingResources() {
        assertTrue(EstadoTurno.RESERVADO.ocupaRecursos());
        assertTrue(EstadoTurno.CONFIRMADO.ocupaRecursos());
        assertFalse(EstadoTurno.CANCELADO_PACIENTE.ocupaRecursos());
        assertFalse(EstadoTurno.CANCELADO_PENALIZADO.ocupaRecursos());
        assertFalse(EstadoTurno.PENDIENTE_REPROGRAMACION.ocupaRecursos());
        assertFalse(EstadoTurno.ATENDIDO.ocupaRecursos());
        assertFalse(EstadoTurno.AUSENTE.ocupaRecursos());
    }

    @Test
    @DisplayName("CompatibilidadRecursoDTO construye correctamente el contrato inter-servicio (FUNC-05)")
    void shouldBuildCompatibilidadRecursoDTO() {
        CompatibilidadRecursoDTO dto = CompatibilidadRecursoDTO.builder()
                .profesionalId(1L)
                .servicioId(2L)
                .duracionMinutos(45)
                .costo(new BigDecimal("32000.00"))
                .sedeId(1L)
                .consultorioFijoId(1L)
                .equipamientosIds(List.of(1L, 2L))
                .build();

        assertEquals(45, dto.getDuracionMinutos());
        assertEquals(new BigDecimal("32000.00"), dto.getCosto());
        assertEquals(1L, dto.getSedeId());
        assertEquals(1L, dto.getConsultorioFijoId());
        assertEquals(List.of(1L, 2L), dto.getEquipamientosIds());
    }

    @Test
    @DisplayName("GlobalExceptionHandler traduce las excepciones de negocio RN-01 a RN-07 a sus códigos HTTP exactos")
    void shouldMapBusinessExceptionsToExpectedHttpStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/catalogo/compatibilidad");
        request.addHeader(CorrelationIdConstants.CORRELATION_ID_HEADER, "corr-test-123");

        // RN-01 -> 422 Unprocessable Entity
        ResponseEntity<ErrorResponseDTO> rn01 = exceptionHandler.handleMediTurnosException(
                new ReglaNegocioException("El profesional no atiende en el horario solicitado"), request);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, rn01.getStatusCode());
        assertNotNull(rn01.getBody());
        assertEquals("RN-01", rn01.getBody().getRuleCode());
        assertEquals("corr-test-123", rn01.getBody().getCorrelationId());

        // RN-02 (Recurso en mantenimiento) -> 409 Conflict
        ResponseEntity<ErrorResponseDTO> rn02Mant = exceptionHandler.handleMediTurnosException(
                new RecursoNoDisponibleException("El equipamiento se encuentra en mantenimiento"), request);
        assertEquals(HttpStatus.CONFLICT, rn02Mant.getStatusCode());
        assertEquals("RN-02", rn02Mant.getBody().getRuleCode());

        // RN-02 (Coherencia de sede) -> 422 Unprocessable Entity
        ResponseEntity<ErrorResponseDTO> rn02Sede = exceptionHandler.handleMediTurnosException(
                new CoherenciaSedeException("El consultorio y el equipamiento pertenecen a sedes distintas"), request);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, rn02Sede.getStatusCode());
        assertEquals("RN-02", rn02Sede.getBody().getRuleCode());

        // RN-03 (Colisión horaria tripartita) -> 409 Conflict
        ResponseEntity<ErrorResponseDTO> rn03 = exceptionHandler.handleMediTurnosException(
                new ColisionHorariaException("Colisión en consultorio compartido Box 101"), request);
        assertEquals(HttpStatus.CONFLICT, rn03.getStatusCode());
        assertEquals("RN-03", rn03.getBody().getRuleCode());

        // RN-04 (Monto inválido) -> 400 Bad Request
        ResponseEntity<ErrorResponseDTO> rn04 = exceptionHandler.handleMediTurnosException(
                new EstadoTurnoInvalidoException("El monto no coincide con el costo acordado", HttpStatus.BAD_REQUEST, "RN-04"), request);
        assertEquals(HttpStatus.BAD_REQUEST, rn04.getStatusCode());
        assertEquals("RN-04", rn04.getBody().getRuleCode());

        // 404 Not Found
        ResponseEntity<ErrorResponseDTO> notFound = exceptionHandler.handleMediTurnosException(
                new RecursoNoEncontradoException("Profesional no encontrado"), request);
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
    }

    @Test
    @DisplayName("CorrelationIdFilter propaga o genera X-Correlation-Id y limpia el MDC al finalizar")
    void shouldGenerateAndCleanCorrelationIdInFilter() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/reservas/turnos");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcInsideChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcInsideChain.set(MDC.get(CorrelationIdConstants.CORRELATION_ID_MDC_KEY));

        filter.doFilter(request, response, chain);

        String responseHeader = response.getHeader(CorrelationIdConstants.CORRELATION_ID_HEADER);
        assertNotNull(responseHeader);
        assertFalse(responseHeader.isBlank());
        assertEquals(responseHeader, mdcInsideChain.get());
        assertNull(MDC.get(CorrelationIdConstants.CORRELATION_ID_MDC_KEY), "El MDC debe limpiarse luego del filtro");
    }
}
