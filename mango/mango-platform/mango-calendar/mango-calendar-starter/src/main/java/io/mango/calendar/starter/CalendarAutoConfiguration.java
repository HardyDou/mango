package io.mango.calendar.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mango.calendar", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.calendar.core.mapper")
@ComponentScan({
        "io.mango.calendar.core.service",
        "io.mango.calendar.starter.controller"
})
public class CalendarAutoConfiguration {
}
