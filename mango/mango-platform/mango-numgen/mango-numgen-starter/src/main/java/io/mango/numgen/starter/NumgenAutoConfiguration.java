package io.mango.numgen.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mango.numgen", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.numgen.core.mapper")
@ComponentScan({
        "io.mango.numgen.core.service",
        "io.mango.numgen.starter.controller"
})
public class NumgenAutoConfiguration {
}
