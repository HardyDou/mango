package io.mango.infra.context.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Context propagation configuration properties.
 *
 * @author Mango
 */
@ConfigurationProperties(prefix = "mango.context")
public class ContextProperties {

    /**
     * Whether to enable context propagation.
     * When enabled, TtlExecutorDecorator wraps raw executors automatically.
     */
    private boolean enabled = true;
}
