package io.mango.job.starter;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import io.mango.job.starter.probe.MangoJobRuntimeProbeHandler;

/**
 * Mango Job 自动配置。
 */
@Configuration
@ConditionalOnProperty(prefix = "mango.job", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan(basePackages = {
        "io.mango.job.core.mapper",
        "io.mango.job.starter.powerjob"
}, annotationClass = Mapper.class)
@ComponentScan({
        "io.mango.job.core.service",
        "io.mango.job.starter.controller"
})
@Import(io.mango.job.starter.powerjob.PowerJobAutoConfiguration.class)
public class JobAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "mango.job.probe", name = "enabled", havingValue = "true")
    MangoJobRuntimeProbeHandler mangoJobRuntimeProbeHandler() {
        return new MangoJobRuntimeProbeHandler();
    }
}
