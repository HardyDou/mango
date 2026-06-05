package io.mango.job.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Mango Job 自动配置。
 */
@Configuration
@ConditionalOnProperty(prefix = "mango.job", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.job.core.mapper")
@ComponentScan({
        "io.mango.job.core.service"
})
public class JobAutoConfiguration {
}
