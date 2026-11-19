package utn.frc.backend.mediturnos.reservas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;
import utn.frc.backend.mediturnos.common.enums.CanalNotificacion;
import utn.frc.backend.mediturnos.common.enums.EstadoEnvioNotificacion;
import utn.frc.backend.mediturnos.common.enums.EstadoTurno;
import utn.frc.backend.mediturnos.common.enums.TipoAlertaNotificacion;
import utn.frc.backend.mediturnos.reservas.client.GoogleCalendarClient;
import utn.frc.backend.mediturnos.reservas.entity.Notificacion;
import utn.frc.backend.mediturnos.reservas.entity.Paciente;
import utn.frc.backend.mediturnos.reservas.entity.Turno;
import utn.frc.backend.mediturnos.reservas.repository.NotificacionRepository;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificacionServiceGoogleCalendarTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private RestClient notificacionRestClient;

    @Mock
    private GoogleCalendarClient googleCalendarClient;

    private NotificacionService service;
    private Paciente paciente;
    private Turno turno;

    @BeforeEach
    void setUp() {
        service = new NotificacionService(notificacionRepository, notificacionRestClient, googleCalendarClient);
        paciente = Paciente.builder().id(1L).nombre("Ana").apellido("Gómez")
                .email("ana@example.com").telefonoWhatsapp("123").build();
        turno = Turno.builder().id(42L).paciente(paciente).estado(EstadoTurno.RESERVADO)
                .fecha(LocalDate.of(2026, 11, 20)).horaInicio(LocalTime.of(10, 0)).horaFin(LocalTime.of(10, 30)).build();
        when(googleCalendarClient.isEnabled()).thenReturn(true);
        when(googleCalendarClient.eventIdPara(any())).thenReturn("mediturnos42");
    }

    private Notificacion auditoriaGoogle() {
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
                .filter(n -> n.getCanal() == CanalNotificacion.GOOGLE_CALENDAR)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se auditó el canal GOOGLE_CALENDAR"));
    }

    @Test
    @DisplayName("RESERVA crea el evento con invitación al email del paciente y audita ENVIADO")
    void reservaCreaEvento() {
        service.sincronizarGoogleCalendarResiliente(turno, paciente, TipoAlertaNotificacion.RESERVA, "msg");

        verify(googleCalendarClient).crearOActualizarEvento(turno, paciente, "msg");
        Notificacion n = auditoriaGoogle();
        assertEquals(EstadoEnvioNotificacion.ENVIADO, n.getEstadoEnvio());
        assertEquals("ana@example.com", n.getDestinatario());
    }

    @Test
    @DisplayName("CANCELACION cancela el evento")
    void cancelacionCancelaEvento() {
        service.sincronizarGoogleCalendarResiliente(turno, paciente, TipoAlertaNotificacion.CANCELACION, "msg");

        verify(googleCalendarClient).cancelarEvento(turno);
        verify(googleCalendarClient, never()).crearOActualizarEvento(any(), any(), anyString());
    }

    @Test
    @DisplayName("REPROGRAMACION con turno PENDIENTE_REPROGRAMACION no toca el calendario")
    void pendienteReprogramacionNoSincroniza() {
        turno.setEstado(EstadoTurno.PENDIENTE_REPROGRAMACION);

        service.sincronizarGoogleCalendarResiliente(turno, paciente, TipoAlertaNotificacion.REPROGRAMACION, "msg");

        verifyNoInteractions(notificacionRepository);
        verify(googleCalendarClient, never()).crearOActualizarEvento(any(), any(), anyString());
        verify(googleCalendarClient, never()).cancelarEvento(any());
    }

    @Test
    @DisplayName("RN-08: un fallo de Google no se propaga y se audita como ERROR")
    void falloGoogleSeDegradaControladamente() {
        doThrow(new RuntimeException("timeout")).when(googleCalendarClient).crearOActualizarEvento(any(), any(), anyString());

        assertDoesNotThrow(() ->
                service.sincronizarGoogleCalendarResiliente(turno, paciente, TipoAlertaNotificacion.RESERVA, "msg"));

        Notificacion n = auditoriaGoogle();
        assertEquals(EstadoEnvioNotificacion.ERROR, n.getEstadoEnvio());
        assertTrue(n.getDetalleMensaje().contains("timeout"));
    }

    @Test
    @DisplayName("Modo simulado (enabled=false): no llama a Google y audita ENVIADO [SIMULADO]")
    void modoSimulado() {
        when(googleCalendarClient.isEnabled()).thenReturn(false);

        service.enviarNotificacionResiliente(turno, paciente, TipoAlertaNotificacion.RESERVA, "msg");

        verify(googleCalendarClient, never()).crearOActualizarEvento(any(), any(), anyString());
        Notificacion n = auditoriaGoogle();
        assertEquals(EstadoEnvioNotificacion.ENVIADO, n.getEstadoEnvio());
        assertTrue(n.getDetalleMensaje().contains("[SIMULADO]"));
    }
}
