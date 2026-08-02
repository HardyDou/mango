package io.mango.job.starter;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import io.mango.job.support.handler.MangoJobHandler;
import io.mango.job.support.service.MangoJobHandlerRegistry;
import io.mango.job.starter.probe.MangoJobRuntimeProbeHandler;
import io.mango.job.core.service.nativeengine.MangoJobIdempotencyKeys;
import io.mango.job.core.service.nativeengine.MangoJobLeaseManager;
import io.mango.job.support.nativeengine.InMemoryMangoJobWorkerTransport;
import io.mango.job.support.nativeengine.MangoJobWorkerExecutor;
import io.mango.job.core.service.nativeengine.MangoJobWorkerTransportRegistry;
import io.mango.job.support.nativeengine.IMangoJobWorkerTransport;
import io.mango.job.support.nativeengine.MangoNativeJobProperties;

import java.util.List;

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
        "io.mango.job.core.resource",
        "io.mango.job.core.service",
        "io.mango.job.starter.controller",
        "io.mango.job.starter.nativeengine",
        "io.mango.job.starter.resource"
})
public class JobAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MangoJobHandlerRegistry mangoJobHandlerRegistry(ObjectProvider<MangoJobHandler> handlers,
                                                     MangoNativeJobProperties properties,
                                                     @Value("${spring.application.name:}") String applicationName) {
        return new MangoJobHandlerRegistry(handlers.orderedStream().toList(), properties, applicationName);
    }

    @Bean
    @ConditionalOnMissingBean
    MangoJobWorkerExecutor mangoJobWorkerExecutor(MangoJobHandlerRegistry handlerRegistry,
                                                  MangoNativeJobProperties properties) {
        return new MangoJobWorkerExecutor(handlerRegistry, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    InMemoryMangoJobWorkerTransport inMemoryMangoJobWorkerTransport(
            MangoJobWorkerExecutor workerExecutor) {
        return new InMemoryMangoJobWorkerTransport(workerExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    MangoJobWorkerTransportRegistry mangoJobWorkerTransportRegistry(
            List<IMangoJobWorkerTransport> transports) {
        return new MangoJobWorkerTransportRegistry(transports);
    }

    @Bean
    @ConditionalOnMissingBean
    MangoJobIdempotencyKeys mangoJobIdempotencyKeys() {
        return new MangoJobIdempotencyKeys();
    }

    @Bean
    @ConditionalOnMissingBean
    MangoJobLeaseManager mangoJobLeaseManager() {
        return new MangoJobLeaseManager();
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.job.probe", name = "enabled", havingValue = "true", matchIfMissing = true)
    MangoJobRuntimeProbeHandler mangoJobRuntimeProbeHandler() {
        return new MangoJobRuntimeProbeHandler();
    }
}
