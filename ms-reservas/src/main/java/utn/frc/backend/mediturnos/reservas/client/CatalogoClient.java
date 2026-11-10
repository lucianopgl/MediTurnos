package utn.frc.backend.mediturnos.reservas.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import utn.frc.backend.mediturnos.common.dto.BloqueoAgendaRequestDTO;
import utn.frc.backend.mediturnos.common.dto.CompatibilidadRecursoDTO;
import utn.frc.backend.mediturnos.common.dto.ErrorResponseDTO;
import utn.frc.backend.mediturnos.common.exception.CoherenciaSedeException;
import utn.frc.backend.mediturnos.common.exception.RecursoNoDisponibleException;
import utn.frc.backend.mediturnos.common.exception.RecursoNoEncontradoException;
import utn.frc.backend.mediturnos.common.exception.ReglaNegocioException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Cliente HTTP síncrono RestClient hacia ms-catalogo-recursos.
 * Implementa la comunicación unidireccional y traduce respuestas de error estandarizadas (ADR 002).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogoClient {

    private final RestClient catalogoRestClient;
    private final ObjectMapper objectMapper;

    /**
     * Valida reglas estáticas y parametrizadas (RN-01 y RN-02) en catálogo y obtiene IDs de recursos (FUNC-05).
     */
    public CompatibilidadRecursoDTO obtenerCompatibilidad(Long profesionalId, Long servicioId,
                                                          LocalDate fecha, LocalTime horaInicio) {
        return catalogoRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/compatibilidad")
                        .queryParam("profesionalId", profesionalId)
                        .queryParam("servicioId", servicioId)
                        .queryParam("fecha", fecha)
                        .queryParam("horaInicio", horaInicio)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> manejarErrorCatalogo(response))
                .body(CompatibilidadRecursoDTO.class);
    }

    /**
     * Registra un bloqueo médico puntual en BLOQUEOS_PROFESIONAL (FUNC-10 / RN-06).
     */
    public void registrarBloqueoAgenda(Long profesionalId, BloqueoAgendaRequestDTO bloqueoDTO) {
        catalogoRestClient.post()
                .uri("/profesionales/{id}/bloqueos", profesionalId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(bloqueoDTO)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> manejarErrorCatalogo(response))
                .toBodilessEntity();
    }

    private void manejarErrorCatalogo(ClientHttpResponse response) throws IOException {
        String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        String mensajeError = "Error en comunicación con catálogo";
        String ruleCode = null;

        try {
            ErrorResponseDTO errorDTO = objectMapper.readValue(responseBody, ErrorResponseDTO.class);
            if (errorDTO.getMessage() != null) {
                mensajeError = errorDTO.getMessage();
            }
            ruleCode = errorDTO.getRuleCode();
        } catch (Exception ignored) {
            if (!responseBody.isBlank()) {
                mensajeError = responseBody;
            }
        }

        HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
        log.warn("Error recibido de ms-catalogo-recursos [{} - {}]: {}", status, ruleCode, mensajeError);

        if (status == HttpStatus.NOT_FOUND) {
            throw new RecursoNoEncontradoException(mensajeError);
        } else if (status == HttpStatus.CONFLICT) {
            throw new RecursoNoDisponibleException(mensajeError);
        } else if (status == HttpStatus.UNPROCESSABLE_ENTITY) {
            if ("RN-02".equalsIgnoreCase(ruleCode) || mensajeError.toLowerCase().contains("sede")) {
                throw new CoherenciaSedeException(mensajeError);
            }
            throw new ReglaNegocioException(mensajeError);
        } else {
            throw new ReglaNegocioException("Error devuelto por catálogo (" + response.getStatusCode() + "): " + mensajeError);
        }
    }
}
