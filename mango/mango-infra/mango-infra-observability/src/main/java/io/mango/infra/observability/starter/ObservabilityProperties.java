package io.mango.infra.observability.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Observability Properties
 *
 * @author Mango
 */
@Data
@ConfigurationProperties(prefix = "mango.observability")
public class ObservabilityProperties {

    /**
     * Enable observability
     */
    private boolean enabled = true;

    /**
     * Metrics configuration
     */
    private Metrics metrics = new Metrics();

    /**
     * Tracing configuration
     */
    private Tracing tracing = new Tracing();

    @Data
    public static class Metrics {
        /**
         * Enable metrics collection
         */
        private boolean enabled = true;

        /**
         * Export to OTLP endpoint
         */
        private boolean exportToOtlp = false;

        /**
         * OTLP endpoint URL
         */
        private String otlpEndpoint = "http://localhost:4317";
    }

    @Data
    public static class Tracing {
        /**
         * Enable distributed tracing
         */
        private boolean enabled = true;

        /**
         * Sampling probability (0.0 - 1.0)
         */
        private float samplingProbability = 0.1f;
    }
}
