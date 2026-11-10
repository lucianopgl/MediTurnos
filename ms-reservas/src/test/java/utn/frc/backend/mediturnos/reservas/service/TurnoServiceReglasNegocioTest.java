package utn.frc.backend.mediturnos.reservas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import utn.frc.backend.mediturnos.common.dto.CompatibilidadRecursoDTO;
import utn.frc.backend.mediturnos.common.enums.EstadoPago;
import utn.frc.backend.mediturnos.common.enums.EstadoTurno;
import utn.frc.backend.mediturnos.common.enums.TipoAlertaNotificacion;
import utn.frc.backend.mediturnos.common.exception.ColisionHorariaException;
import utn.frc.backend.mediturnos.common.exception.EstadoTurnoInvalidoException;
import utn.frc.backend.mediturnos.reservas.client.CatalogoClient;
import utn.frc.backend.mediturnos.reservas.dto.PagoRequestDTO;
import utn.frc.backend.mediturnos.reservas.dto.PagoResponseDTO;
import utn.frc.backend.mediturnos.reservas.dto.ReprogramacionTurnoRequestDTO;
import utn.frc.backend.mediturnos.reservas.dto.TurnoRequestDTO;
import utn.frc.backend.mediturnos.reservas.dto.TurnoResponseDTO;
import utn.frc.backend.mediturnos.reservas.entity.Pago;
import utn.frc.backend.mediturnos.reservas.entity.Paciente;
import utn.frc.backend.mediturnos.reservas.entity.Turno;
import utn.frc.backend.mediturnos.reservas.repository.PagoRepository;
import utn.frc.backend.mediturnos.reservas.repository.TurnoRepository;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnoServiceReglasNegocioTest {

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private PacienteService pacienteService;

    @Mock
    private ValidadorColisionService validadorColisionService;

    @Mock
    private CatalogoClient catalogoClient;

    @Mock
    private NotificacionService notificacionService;

    private Clock clock;
    private TurnoService turnoService;

    private Paciente pacienteTest;
    private final Instant fixedInstant = Instant.parse("2026-11-15T10:00:00Z");
    private final ZoneId zoneId = ZoneId.of("UTC");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(fixedInstant, zoneId);
        turnoService = new TurnoService(
                turnoRepository,
                pagoRepository,
                pacienteService,
                validadorColisionService,
                catalogoClient,
                notificacionService,
                clock
        );

        pacienteTest = Paciente.builder()
                .id(1L)
                .keycloakSub("user-sub-001")
                .dni("40111222")
                .nombre("Juan")
                .apellido("Pérez")
                .email("juan.perez@test.com")
                .telefonoWhatsapp("+5493515550101")
                .cantidadPenalizaciones(0)
                .activo(true)
                .build();
    }

    private void mockSecurityContext(String subject, String role) {
        Jwt jwt = mock(Jwt.class);
        lenient().when(jwt.getSubject()).thenReturn(subject);

        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getPrincipal()).thenReturn(jwt);
        lenient().doReturn(List.of(new SimpleGrantedAuthority(role))).when(auth).getAuthorities();

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("CV-01: Solicitar y reservar atómicamente un turno exitoso (RN-01, RN-02, RN-03, RN-08)")
    void testSolicitarTurnoExitoso() {
        mockSecurityContext("user-sub-001", "ROL_PACIENTE");
        when(pacienteService.obtenerEntidadAutenticada()).thenReturn(pacienteTest);

        TurnoRequestDTO request = TurnoRequestDTO.builder()
                .profesionalId(1L)
                .servicioId(2L)
                .fecha(LocalDate.of(2026, 11, 20))
                .horaInicio(LocalTime.of(9, 0))
                .build();

        CompatibilidadRecursoDTO compatibilidad = CompatibilidadRecursoDTO.builder()
                .profesionalId(1L)
                .servicioId(2L)
                .duracionMinutos(45)
                .costo(new BigDecimal("32000.00"))
                .sedeId(1L)
                .consultorioFijoId(1L)
                .equipamientosIds(List.of(1L, 2L))
                .build();

        when(catalogoClient.obtenerCompatibilidad(eq(1L), eq(2L), eq(LocalDate.of(2026, 11, 20)), eq(LocalTime.of(9, 0))))
                .thenReturn(compatibilidad);

        doNothing().when(validadorColisionService).validarNoColision(any(), any(), any(), any(), any(), any(), any(), any());

        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocation -> {
            Turno t = invocation.getArgument(0);
            t.setId(10L);
            return t;
        });

        TurnoResponseDTO response = turnoService.solicitarTurno(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(EstadoTurno.RESERVADO, response.getEstado());
        assertFalse(response.getPagado());
        assertEquals(new BigDecimal("32000.00"), response.getCostoAcordado());
        assertEquals(LocalTime.of(9, 45), response.getHoraFin());

        verify(notificacionService).enviarNotificacionResiliente(any(), eq(pacienteTest), eq(TipoAlertaNotificacion.RESERVA), anyString());
    }

    @Test
    @DisplayName("CV-02 / RN-03: Rechazo con ColisionHorariaException ante solapamiento en schema_reservas")
    void testSolicitarTurnoColisionHoraria() {
        mockSecurityContext("user-sub-001", "ROL_PACIENTE");
        when(pacienteService.obtenerEntidadAutenticada()).thenReturn(pacienteTest);

        TurnoRequestDTO request = TurnoRequestDTO.builder()
                .profesionalId(1L)
                .servicioId(1L)
                .fecha(LocalDate.of(2026, 11, 20))
                .horaInicio(LocalTime.of(9, 0))
                .build();

        CompatibilidadRecursoDTO compatibilidad = CompatibilidadRecursoDTO.builder()
                .profesionalId(1L)
                .servicioId(1L)
                .duracionMinutos(30)
                .costo(new BigDecimal("15000.00"))
                .sedeId(1L)
                .consultorioFijoId(1L)
                .equipamientosIds(List.of(2L))
                .build();

        when(catalogoClient.obtenerCompatibilidad(any(), any(), any(), any())).thenReturn(compatibilidad);

        doThrow(new ColisionHorariaException("El consultorio asignado (fijo compartido) se encuentra ocupado por otro turno en esa franja horaria."))
                .when(validadorColisionService).validarNoColision(any(), any(), any(), any(), any(), any(), any(), any());

        ColisionHorariaException ex = assertThrows(ColisionHorariaException.class, () -> turnoService.solicitarTurno(request));
        assertTrue(ex.getMessage().contains("consultorio asignado"));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CV-04 / RN-04: Confirmación de pago aprobada pasa turno RESERVADO a CONFIRMADO (pagado = true)")
    void testRegistrarPagoExitoso() {
        mockSecurityContext("user-sub-001", "ROL_PACIENTE");

        Turno turno = Turno.builder()
                .id(1L)
                .paciente(pacienteTest)
                .profesionalId(1L)
                .costoAcordado(new BigDecimal("15000.00"))
                .estado(EstadoTurno.RESERVADO)
                .pagado(false)
                .fecha(LocalDate.of(2026, 11, 20))
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(9, 30))
                .build();

        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));
        when(pagoRepository.existsByComprobanteReferencia("PAY-123")).thenReturn(false);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> {
            Pago p = i.getArgument(0);
            p.setId(100L);
            return p;
        });

        PagoRequestDTO request = PagoRequestDTO.builder()
                .comprobanteReferencia("PAY-123")
                .medioPago("TARJETA_DEBITO")
                .monto(new BigDecimal("15000.00"))
                .build();

        PagoResponseDTO response = turnoService.registrarPago(1L, request);

        assertNotNull(response);
        assertEquals(EstadoTurno.CONFIRMADO, response.getEstadoTurnoActual());
        assertTrue(turno.getPagado());
        assertEquals(EstadoTurno.CONFIRMADO, turno.getEstado());
        verify(notificacionService).enviarNotificacionResiliente(eq(turno), eq(pacienteTest), eq(TipoAlertaNotificacion.CONFIRMACION), anyString());
    }

    @Test
    @DisplayName("CV-04 / RN-04: Rechazo de pago si el monto no coincide con costoAcordado")
    void testRegistrarPagoMontoInvalido() {
        mockSecurityContext("user-sub-001", "ROL_PACIENTE");

        Turno turno = Turno.builder()
                .id(1L)
                .paciente(pacienteTest)
                .costoAcordado(new BigDecimal("15000.00"))
                .estado(EstadoTurno.RESERVADO)
                .pagado(false)
                .build();

        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));

        PagoRequestDTO request = PagoRequestDTO.builder()
                .comprobanteReferencia("PAY-123")
                .medioPago("TARJETA_DEBITO")
                .monto(new BigDecimal("10000.00")) // monto incorrecto
                .build();

        assertThrows(EstadoTurnoInvalidoException.class, () -> turnoService.registrarPago(1L, request));
    }

    @Test
    @DisplayName("CV-05 / RN-05: Cancelación anticipada (>= 24 hs) pasa a CANCELADO_PACIENTE sin penalización")
    void testCancelarTurnoAnticipadoSinPenalidad() {
        mockSecurityContext("user-sub-001", "ROL_PACIENTE");

        // Turno en fecha 2026-11-20 (más de 24 hs respecto al reloj fijo 2026-11-15T10:00:00Z)
        Turno turno = Turno.builder()
                .id(1L)
                .paciente(pacienteTest)
                .fecha(LocalDate.of(2026, 11, 20))
                .horaInicio(LocalTime.of(10, 0))
                .estado(EstadoTurno.CONFIRMADO)
                .pagado(true)
                .build();

        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));

        TurnoResponseDTO response = turnoService.cancelarTurnoPaciente(1L);

        assertEquals(EstadoTurno.CANCELADO_PACIENTE, response.getEstado());
        assertEquals(0, pacienteTest.getCantidadPenalizaciones());
        verify(notificacionService).enviarNotificacionResiliente(eq(turno), eq(pacienteTest), eq(TipoAlertaNotificacion.CANCELACION), anyString());
    }

    @Test
    @DisplayName("CV-05 / RN-05: Cancelación tardía (< 24 hs) pasa a CANCELADO_PENALIZADO y suma +1 penalización")
    void testCancelarTurnoTardioConPenalidad() {
        mockSecurityContext("user-sub-001", "ROL_PACIENTE");

        // Turno el 2026-11-15 a las 18:00 hs (faltan solo 8 hs respecto al reloj fijo 2026-11-15T10:00:00Z)
        Turno turno = Turno.builder()
                .id(2L)
                .paciente(pacienteTest)
                .fecha(LocalDate.of(2026, 11, 15))
                .horaInicio(LocalTime.of(18, 0))
                .estado(EstadoTurno.CONFIRMADO)
                .pagado(true)
                .build();

        when(turnoRepository.findById(2L)).thenReturn(Optional.of(turno));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));

        TurnoResponseDTO response = turnoService.cancelarTurnoPaciente(2L);

        assertEquals(EstadoTurno.CANCELADO_PENALIZADO, response.getEstado());
        assertEquals(1, pacienteTest.getCantidadPenalizaciones());
        verify(notificacionService).enviarNotificacionResiliente(eq(turno), eq(pacienteTest), eq(TipoAlertaNotificacion.CANCELACION), anyString());
    }

    @Test
    @DisplayName("CV-07 / RN-07: Reprogramación prioritaria de turno pagado conserva costo y pasa directo a CONFIRMADO")
    void testReprogramacionPrioritariaSinDobleCobro() {
        mockSecurityContext("user-sub-001", "ROL_PACIENTE");

        Turno turno = Turno.builder()
                .id(3L)
                .paciente(pacienteTest)
                .profesionalId(1L)
                .servicioId(1L)
                .consultorioId(1L)
                .sedeId(1L)
                .fecha(LocalDate.of(2026, 11, 18))
                .horaInicio(LocalTime.of(11, 0))
                .horaFin(LocalTime.of(11, 30))
                .costoAcordado(new BigDecimal("15000.00"))
                .estado(EstadoTurno.PENDIENTE_REPROGRAMACION)
                .pagado(true)
                .equipamientos(new ArrayList<>())
                .build();

        when(turnoRepository.findById(3L)).thenReturn(Optional.of(turno));

        LocalDate nuevaFecha = LocalDate.of(2026, 11, 25);
        LocalTime nuevaHora = LocalTime.of(10, 0);

        CompatibilidadRecursoDTO compatibilidad = CompatibilidadRecursoDTO.builder()
                .profesionalId(1L)
                .servicioId(1L)
                .duracionMinutos(30)
                .costo(new BigDecimal("18000.00")) // Precio nuevo en catálogo, pero se conserva el original
                .sedeId(1L)
                .consultorioFijoId(1L)
                .equipamientosIds(List.of(2L))
                .build();

        when(catalogoClient.obtenerCompatibilidad(eq(1L), eq(1L), eq(nuevaFecha), eq(nuevaHora))).thenReturn(compatibilidad);
        doNothing().when(validadorColisionService).validarNoColision(any(), any(), any(), any(), any(), any(), any(), eq(3L));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));

        ReprogramacionTurnoRequestDTO request = ReprogramacionTurnoRequestDTO.builder()
                .nuevaFecha(nuevaFecha)
                .nuevaHoraInicio(nuevaHora)
                .build();

        TurnoResponseDTO response = turnoService.reprogramarTurno(3L, request);

        assertEquals(EstadoTurno.CONFIRMADO, response.getEstado());
        assertTrue(response.getPagado());
        // RN-07: Conserva el costo acordado original
        assertEquals(new BigDecimal("15000.00"), response.getCostoAcordado());
        assertEquals(nuevaFecha, response.getFecha());
        assertEquals(nuevaHora, response.getHoraInicio());
        verify(notificacionService).enviarNotificacionResiliente(eq(turno), eq(pacienteTest), eq(TipoAlertaNotificacion.REPROGRAMACION), anyString());
    }
}
