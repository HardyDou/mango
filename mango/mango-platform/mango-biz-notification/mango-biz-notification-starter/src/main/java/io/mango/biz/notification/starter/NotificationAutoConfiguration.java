package io.mango.biz.notification.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mango.message", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.biz.notification.core.mapper")
@ComponentScan({
        "io.mango.biz.notification.core.controller",
        "io.mango.biz.notification.core.service"
})
public class NotificationAutoConfiguration {
    // Messaging protocol beans are provided by mango-infra-realtime-starter.
}
