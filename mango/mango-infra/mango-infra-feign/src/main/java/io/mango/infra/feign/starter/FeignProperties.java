package io.mango.infra.feign.starter;

import feign.Logger;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feign properties configuration
 *
 * @author Mango
 */
@Data
@ConfigurationProperties(prefix = "mango.feign")
public class FeignProperties {

    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 10_000;
    private static final int DEFAULT_RETRY_ATTEMPTS = 3;

    /**
     * Initial retry period in milliseconds. The property name is retained for compatibility.
     */
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT_MILLIS;

    /**
     * Maximum retry period in milliseconds. The property name is retained for compatibility.
     */
    private int readTimeout = DEFAULT_READ_TIMEOUT_MILLIS;

    /**
     * Maximum retry attempts.
     */
    private int retry = DEFAULT_RETRY_ATTEMPTS;

    /**
     * Logger level for Feign clients
     */
    private Logger.Level loggerLevel = Logger.Level.BASIC;

    /**
     * Enable Feign request interceptor for tenant/trace context propagation
     */
    private boolean interceptorEnabled = true;

    /**
     * Enable module-name target rewriting.
     */
    private boolean moduleTargetEnabled = true;
}
