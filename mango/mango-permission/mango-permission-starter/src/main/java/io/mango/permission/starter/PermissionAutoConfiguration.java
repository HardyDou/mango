package io.mango.permission.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Permission service auto configuration
 *
 * @author Mango
 */
@Configuration
@MapperScan("io.mango.permission.core.mapper")
@ComponentScan({
        "io.mango.permission.core.service",
        "io.mango.permission.core.service.impl",
        "io.mango.permission.starter.controller"
})
public class PermissionAutoConfiguration {
}
