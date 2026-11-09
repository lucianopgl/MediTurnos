package utn.frc.backend.mediturnos.common.filter;

/**
 * Constantes compartidas para la trazabilidad distribuida mediante el header X-Correlation-Id
 * y el contexto de diagnóstico mapeado (MDC) de SLF4J.
 */
public final class CorrelationIdConstants {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private CorrelationIdConstants() {
        // Clase de constantes no instanciable
    }
}
