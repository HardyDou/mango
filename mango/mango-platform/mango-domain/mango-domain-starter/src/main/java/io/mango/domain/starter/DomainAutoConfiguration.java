package io.mango.domain.starter;

import io.mango.domain.core.mapper.DomainMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * 业务域自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(DomainMapper.class)
@ConditionalOnProperty(prefix = "mango.domain", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.domain.core.mapper")
@ComponentScan({
    "io.mango.domain.core.service",
    "io.mango.domain.starter"
})
public class DomainAutoConfiguration {
}
