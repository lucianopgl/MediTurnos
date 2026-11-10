package utn.frc.backend.mediturnos.reservas.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import utn.frc.backend.mediturnos.common.filter.CorrelationIdConstants;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${catalogo.base-url:http://localhost:8081/api/catalogo}")
    private String catalogoBaseUrl;

    @Value("${notificaciones.api.url:https://api.telegram.org}")
    private String notificacionesApiUrl;

    @Value("${notificaciones.api.connect-timeout-ms:3000}")
    private int notificacionesConnectTimeoutMs;

    @Value("${notificaciones.api.read-timeout-ms:4000}")
    private int notificacionesReadTimeoutMs;

    @Bean
    public RestClient catalogoRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return builder
                .baseUrl(catalogoBaseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    // Propagación de X-Correlation-Id desde MDC
                    String correlationId = MDC.get(CorrelationIdConstants.CORRELATION_ID_MDC_KEY);
                    if (correlationId != null) {
                        request.getHeaders().set(CorrelationIdConstants.CORRELATION_ID_HEADER, correlationId);
                    }

                    // Propagación del Token Bearer si la petición HTTP actual está autenticada
                    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attrs != null) {
                        String authHeader = attrs.getRequest().getHeader("Authorization");
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            request.getHeaders().set("Authorization", authHeader);
                        }
                    }

                    return execution.execute(request, body);
                })
                .build();
    }

    @Bean
    public RestClient notificacionRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(notificacionesConnectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(notificacionesReadTimeoutMs));

        return builder
                .baseUrl(notificacionesApiUrl)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = MDC.get(CorrelationIdConstants.CORRELATION_ID_MDC_KEY);
                    if (correlationId != null) {
                        request.getHeaders().set(CorrelationIdConstants.CORRELATION_ID_HEADER, correlationId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
