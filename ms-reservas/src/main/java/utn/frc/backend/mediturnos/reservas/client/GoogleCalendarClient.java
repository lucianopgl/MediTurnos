package utn.frc.backend.mediturnos.reservas.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import utn.frc.backend.mediturnos.reservas.entity.Paciente;
import utn.frc.backend.mediturnos.reservas.entity.Turno;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP (RestClient) hacia la API pública de Google Calendar v3.
 * <p>
 * Crea, actualiza y cancela el evento del turno en el calendario del centro médico,
 * agregando al paciente como invitado con {@code sendUpdates=all}: Google le envía el
 * email de invitación/actualización/cancelación y el evento aparece en su calendario.
 * <p>
 * Autenticación: OAuth 2.0 con refresh token de la cuenta del centro médico
 * (scope {@code https://www.googleapis.com/auth/calendar.events}). El access token se
 * cachea en memoria hasta poco antes de su vencimiento.
 * <p>
 * El ID del evento es determinístico ({@code <prefijo><turnoId>}), por lo que no se requiere
 * persistir el ID de Google en la base para poder actualizar o cancelar el evento.
 * <p>
 * Este cliente propaga las excepciones; la degradación controlada (RN-08) la aplica
 * {@code NotificacionService}.
 */
@Slf4j
@Component
public class GoogleCalendarClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events";
    private static final String EVENT_URL = EVENTS_URL + "/{eventId}?sendUpdates=all";

    private final RestClient restClient;
    private final Clock clock;

    @Value("${google.calendar.enabled:false}")
    private boolean enabled;

    @Value("${google.calendar.client-id:}")
    private String clientId;

    @Value("${google.calendar.client-secret:}")
    private String clientSecret;

    @Value("${google.calendar.refresh-token:}")
    private String refreshToken;

    @Value("${google.calendar.calendar-id:primary}")
    private String calendarId;

    @Value("${google.calendar.time-zone:America/Argentina/Cordoba}")
    private String timeZone;

    @Value("${google.calendar.event-id-prefix:mediturnos}")
    private String eventIdPrefix;

    private String accessToken;
    private Instant accessTokenExpiresAt = Instant.EPOCH;

    public GoogleCalendarClient(@Qualifier("googleCalendarRestClient") RestClient restClient, Clock clock) {
        this.restClient = restClient;
        this.clock = clock;
    }

    /** Indica si la integración real está activa; si no, NotificacionService opera en modo simulado. */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Crea o reemplaza (upsert) el evento del turno e invita al paciente.
     * Intenta PUT sobre el ID determinístico; si el evento no existe (404) lo crea con POST.
     */
    public void crearOActualizarEvento(Turno turno, Paciente paciente, String descripcion) {
        String eventId = eventIdPara(turno);
        Map<String, Object> evento = construirEvento(eventId, turno, paciente, descripcion);
        String bearer = "Bearer " + obtenerAccessToken();

        try {
            restClient.put()
                    .uri(EVENT_URL, calendarId, eventId)
                    .header("Authorization", bearer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(evento)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[GOOGLE CALENDAR] Evento {} actualizado para turno #{}", eventId, turno.getId());
        } catch (HttpClientErrorException.NotFound notFound) {
            restClient.post()
                    .uri(EVENTS_URL + "?sendUpdates=all", calendarId)
                    .header("Authorization", bearer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(evento)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[GOOGLE CALENDAR] Evento {} creado para turno #{} (invitación enviada a {})",
                    eventId, turno.getId(), paciente.getEmail());
        }
    }

    /**
     * Cancela el evento del turno notificando al paciente. Si el evento ya no existe, no hace nada.
     */
    public void cancelarEvento(Turno turno) {
        String eventId = eventIdPara(turno);
        try {
            restClient.delete()
                    .uri(EVENT_URL, calendarId, eventId)
                    .header("Authorization", "Bearer " + obtenerAccessToken())
                    .retrieve()
                    .toBodilessEntity();
            log.info("[GOOGLE CALENDAR] Evento {} cancelado para turno #{}", eventId, turno.getId());
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND || ex.getStatusCode() == HttpStatus.GONE) {
                log.info("[GOOGLE CALENDAR] Evento {} inexistente o ya cancelado", eventId);
                return;
            }
            throw ex;
        }
    }

    /**
     * Google exige IDs en base32hex (caracteres a-v y 0-9) de 5 a 1024 caracteres.
     */
    public String eventIdPara(Turno turno) {
        return eventIdPrefix.toLowerCase() + turno.getId();
    }

    private Map<String, Object> construirEvento(String eventId, Turno turno, Paciente paciente, String descripcion) {
        LocalDateTime inicio = LocalDateTime.of(turno.getFecha(), turno.getHoraInicio());
        LocalDateTime fin = LocalDateTime.of(turno.getFecha(), turno.getHoraFin());

        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("id", eventId);
        evento.put("status", "confirmed");
        evento.put("summary", "Turno médico #" + turno.getId() + " — MediTurnos");
        evento.put("description", descripcion);
        evento.put("start", Map.of("dateTime", inicio.toString(), "timeZone", timeZone));
        evento.put("end", Map.of("dateTime", fin.toString(), "timeZone", timeZone));
        evento.put("attendees", List.of(Map.of(
                "email", paciente.getEmail(),
                "displayName", paciente.getNombre() + " " + paciente.getApellido()
        )));
        evento.put("reminders", Map.of(
                "useDefault", false,
                "overrides", List.of(
                        Map.of("method", "email", "minutes", 24 * 60),
                        Map.of("method", "popup", "minutes", 60)
                )
        ));
        return evento;
    }

    /**
     * Obtiene un access token vigente intercambiando el refresh token (grant_type=refresh_token).
     */
    private synchronized String obtenerAccessToken() {
        Instant ahora = clock.instant();
        if (accessToken != null && ahora.isBefore(accessTokenExpiresAt)) {
            return accessToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");

        JsonNode respuesta = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        if (respuesta == null || !respuesta.hasNonNull("access_token")) {
            throw new IllegalStateException("Respuesta de token OAuth de Google sin access_token");
        }

        accessToken = respuesta.get("access_token").asText();
        long expiresIn = respuesta.path("expires_in").asLong(3600);
        // Margen de 60 s para no usar un token a punto de vencer
        accessTokenExpiresAt = ahora.plusSeconds(Math.max(0, expiresIn - 60));
        return accessToken;
    }
}
