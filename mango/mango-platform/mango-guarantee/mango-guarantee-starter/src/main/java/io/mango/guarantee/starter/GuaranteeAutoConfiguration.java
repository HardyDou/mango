package io.mango.guarantee.starter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mango.guarantee", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.guarantee.core.mapper")
@ComponentScan({
        "io.mango.guarantee.core.service",
        "io.mango.guarantee.starter.controller"
})
public class GuaranteeAutoConfiguration {
}
