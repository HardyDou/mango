package io.mango.authorization.starter;

import io.mango.authorization.core.mapper.RoleMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;

/**
 * Authorization service auto configuration
 *
 * @author Mango
 */
@AutoConfiguration
@ConditionalOnClass(RoleMapper.class)
@MapperScan("io.mango.authorization.core.mapper")
@ComponentScan({
        "io.mango.authorization.core.service",
        "io.mango.authorization.starter"
})
public class AuthorizationAutoConfiguration {
}
