package io.mango.identity.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Identity service auto configuration.
 */
@Configuration
@MapperScan("io.mango.identity.core.mapper")
@ComponentScan({
        "io.mango.identity.core.service",
        "io.mango.identity.starter"
})
public class IdentityAutoConfiguration {
}
