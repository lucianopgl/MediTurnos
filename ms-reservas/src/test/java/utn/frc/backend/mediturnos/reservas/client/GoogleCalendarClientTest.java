package utn.frc.backend.mediturnos.reservas.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import utn.frc.backend.mediturnos.reservas.entity.Paciente;
import utn.frc.backend.mediturnos.reservas.entity.Turno;

import java.time.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GoogleCalendarClientTest {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final String TOKEN_JSON = "{\"access_token\":\"ya29.test\",\"expires_in\":3600,\"token_type\":\"Bearer\"}";

    private MockRestServiceServer server;
    private GoogleCalendarClient client;
    private Turno turno;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        Clock clock = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneId.of("UTC"));
        client = new GoogleCalendarClient(builder.build(), clock);

        ReflectionTestUtils.setField(client, "enabled", true);
        ReflectionTestUtils.setField(client, "clientId", "cid");
        ReflectionTestUtils.setField(client, "clientSecret", "csecret");
        ReflectionTestUtils.setField(client, "refreshToken", "rtoken");
        ReflectionTestUtils.setField(client, "calendarId", "primary");
        ReflectionTestUtils.setField(client, "timeZone", "America/Argentina/Cordoba");
        ReflectionTestUtils.setField(client, "eventIdPrefix", "mediturnos");

        paciente = Paciente.builder().id(1L).nombre("Ana").apellido("Gómez").email("ana@example.com").build();
        turno = Turno.builder()
                .id(42L)
                .paciente(paciente)
                .fecha(LocalDate.of(2026, 11, 20))
                .horaInicio(LocalTime.of(10, 0))
                .horaFin(LocalTime.of(10, 30))
                .build();
    }

    @Test
    @DisplayName("Reserva: obtiene token, PUT 404 y crea el evento con POST invitando al paciente (sendUpdates=all)")
    void crearEventoNuevoInvitaAlPaciente() {
        server.expect(once(), requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(java.util.Map.of(
                        "grant_type", "refresh_token", "refresh_token", "rtoken", "client_id", "cid")))
                .andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(EVENTS_URL + "/mediturnos42?sendUpdates=all"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        server.expect(once(), requestTo(EVENTS_URL + "?sendUpdates=all"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer ya29.test"))
                .andExpect(jsonPath("$.id").value("mediturnos42"))
                .andExpect(jsonPath("$.start.dateTime").value("2026-11-20T10:00"))
                .andExpect(jsonPath("$.end.dateTime").value("2026-11-20T10:30"))
                .andExpect(jsonPath("$.start.timeZone").value("America/Argentina/Cordoba"))
                .andExpect(jsonPath("$.attendees[0].email").value("ana@example.com"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.crearOActualizarEvento(turno, paciente, "Reserva confirmada");

        server.verify();
    }

    @Test
    @DisplayName("Reprogramación: si el evento existe se actualiza con PUT y el token se reutiliza")
    void actualizarEventoExistenteReutilizaToken() {
        server.expect(once(), requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(EVENTS_URL + "/mediturnos42?sendUpdates=all"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(EVENTS_URL + "/mediturnos42?sendUpdates=all"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.crearOActualizarEvento(turno, paciente, "a");
        client.crearOActualizarEvento(turno, paciente, "b");

        server.verify();
    }

    @Test
    @DisplayName("Cancelación: DELETE del evento; 410 Gone se considera ya cancelado")
    void cancelarEventoYaCanceladoNoFalla() {
        server.expect(once(), requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(EVENTS_URL + "/mediturnos42?sendUpdates=all"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.GONE));

        assertDoesNotThrow(() -> client.cancelarEvento(turno));
        server.verify();
    }

    @Test
    @DisplayName("Errores 5xx de Google se propagan (la resiliencia RN-08 la aplica NotificacionService)")
    void errorServidorSePropaga() {
        server.expect(once(), requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(EVENTS_URL + "/mediturnos42?sendUpdates=all"))
                .andRespond(withServerError());

        assertThrows(HttpServerErrorException.class, () -> client.crearOActualizarEvento(turno, paciente, "x"));
    }
}
