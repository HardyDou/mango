package io.mango.user.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * User service auto configuration
 *
 * @author Mango
 */
@Configuration
@MapperScan("io.mango.user.core.mapper")
@ComponentScan({
        "io.mango.user.core.service",
        "io.mango.user.core.service.impl",
        "io.mango.user.starter.controller"
})
public class UserAutoConfiguration {
}
