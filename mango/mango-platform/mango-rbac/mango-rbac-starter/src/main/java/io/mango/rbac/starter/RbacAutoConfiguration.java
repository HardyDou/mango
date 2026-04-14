package io.mango.rbac.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * RBAC service auto configuration
 *
 * @author Mango
 */
@Configuration
@MapperScan("io.mango.rbac.core.mapper")
@ComponentScan({
        "io.mango.rbac.core.service",
        "io.mango.rbac.core.service.impl",
        "io.mango.rbac.starter.controller",
        "io.mango.rbac.starter.config"
})
public class RbacAutoConfiguration {
}
