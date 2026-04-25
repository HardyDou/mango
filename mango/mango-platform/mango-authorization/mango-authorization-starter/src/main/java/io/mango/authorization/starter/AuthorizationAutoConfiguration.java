package io.mango.authorization.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Authorization service auto configuration
 *
 * @author Mango
 */
@Configuration
@MapperScan("io.mango.authorization.core.mapper")
@ComponentScan({
        "io.mango.authorization.core.service",
        "io.mango.authorization.starter"
})
public class AuthorizationAutoConfiguration {
}
