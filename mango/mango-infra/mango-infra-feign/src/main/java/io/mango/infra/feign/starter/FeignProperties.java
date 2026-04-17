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

    /**
     * Connection timeout in milliseconds
     */
    private int connectTimeout = 5000;

    /**
     * Read timeout in milliseconds
     */
    private int readTimeout = 10000;

    /**
     * Number of retries
     */
    private int retry = 3;

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
