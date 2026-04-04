package io.mango.message.starter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mango.message", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessageAutoConfiguration {
    // Component scanning handles registration of MessageChannel implementations
    // This class can be extended for additional configuration
}
