package io.mango.infra.context.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Context propagation configuration properties.
 * Currently unused but reserved for future auto-wrapping configuration.
 *
 * @author Mango
 */
@ConfigurationProperties(prefix = "mango.context")
public class ContextProperties {
}
