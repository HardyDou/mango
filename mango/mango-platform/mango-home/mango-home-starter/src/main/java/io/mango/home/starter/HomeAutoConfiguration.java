package io.mango.home.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mango.home", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.home.core.mapper")
@ComponentScan({
    "io.mango.home.core.service",
    "io.mango.home.starter.adapter",
    "io.mango.home.starter.controller"
})
public class HomeAutoConfiguration {
}
