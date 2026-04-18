package io.mango.biz.notification.starter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mango.message", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationAutoConfiguration {
    // Messaging protocol beans are provided by mango-infra-realtime-starter.
}
