package io.mango.org.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Org module auto configuration
 *
 * @author Mango
 */
@Configuration
@MapperScan("io.mango.org.core.mapper")
@ComponentScan({
        "io.mango.org.core.controller",
        "io.mango.org.core.service"
})
public class MangoOrgAutoConfiguration {
}
