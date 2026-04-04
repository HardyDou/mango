package io.mango.area.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Area service auto configuration
 *
 * @author Mango
 */
@Configuration
@MapperScan("io.mango.area.core.mapper")
@ComponentScan({
        "io.mango.area.core.service",
        "io.mango.area.core.service.impl",
        "io.mango.area.core.controller"
})
public class MangoAreaAutoConfiguration {
}
