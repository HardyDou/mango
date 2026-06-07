package io.mango.job.starter;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import io.mango.job.starter.probe.MangoJobRuntimeProbeHandler;
import io.mango.job.core.service.nativeengine.IMangoNativeJobRuntime;
import io.mango.job.core.service.nativeengine.MangoJobIdempotencyKeyService;
import io.mango.job.core.service.nativeengine.MangoJobLeaseService;
import io.mango.job.support.nativeengine.MangoNativeJobProperties;

/**
 * Mango Job 自动配置。
 */
@Configuration
@ConditionalOnProperty(prefix = "mango.job", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MangoNativeJobProperties.class)
@MapperScan(basePackages = {
        "io.mango.job.core.mapper"
}, annotationClass = Mapper.class)
@ComponentScan({
        "io.mango.job.core.service",
        "io.mango.job.support.service",
        "io.mango.job.support.nativeengine",
        "io.mango.job.starter.controller"
})
public class JobAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MangoJobIdempotencyKeyService mangoJobIdempotencyKeyService() {
        return new MangoJobIdempotencyKeyService();
    }

    @Bean
    @ConditionalOnMissingBean
    MangoJobLeaseService mangoJobLeaseService() {
        return new MangoJobLeaseService();
    }

    @Bean
    @ConditionalOnMissingBean
    MangoNativeJobScheduler mangoNativeJobScheduler(IMangoNativeJobRuntime nativeJobRuntime,
                                                    MangoNativeJobProperties properties) {
        return new MangoNativeJobScheduler(nativeJobRuntime, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.job.probe", name = "enabled", havingValue = "true")
    MangoJobRuntimeProbeHandler mangoJobRuntimeProbeHandler() {
        return new MangoJobRuntimeProbeHandler();
    }
}
