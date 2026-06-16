package io.mango.gridlayout.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mango.grid-layout", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.gridlayout.core.mapper")
@ComponentScan({
        "io.mango.gridlayout.core.service",
        "io.mango.gridlayout.starter.controller"
})
public class GridLayoutAutoConfiguration {
}
