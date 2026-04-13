package io.mango.infra.orm.starter;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * ORM auto-configuration
 */
@AutoConfiguration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mango.orm", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OrmProperties.class)
public class OrmAutoConfiguration {

    private final OrmProperties properties;
}
