package io.mango.calendar.starter.remote;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mango.calendar.remote", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableFeignClients(basePackageClasses = CalendarFeignClient.class)
public class CalendarRemoteAutoConfiguration {
}
