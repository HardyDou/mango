package io.mango.infra.context.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Context propagation auto-configuration.
 * <p>
 * Provides {@link TtlExecutorDecorator} for wrapping raw executors with
 * TransmittableThreadLocal context propagation support.
 *
 * @author Mango
 */
@AutoConfiguration
@EnableConfigurationProperties(ContextProperties.class)
@ComponentScan(basePackages = "io.mango.infra.context.starter")
@ConditionalOnProperty(prefix = "mango.context", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ContextPropagationAutoConfiguration {
}
