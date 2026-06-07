package io.mango.job.starter.remote;

import io.mango.job.api.handler.MangoJobHandler;
import io.mango.job.support.service.IMangoJobHandlerRegistry;
import io.mango.job.support.service.MangoJobHandlerRegistry;
import io.mango.job.support.nativeengine.MangoJobWorkerExecutor;
import io.mango.job.support.nativeengine.MangoJobWorkerInternalController;
import io.mango.job.support.nativeengine.MangoNativeJobProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Mango Job 远程调用自动配置。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(MangoNativeJobProperties.class)
@EnableFeignClients(basePackages = "io.mango.job.starter.remote")
public class JobRemoteAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    IMangoJobHandlerRegistry mangoJobHandlerRegistry(ObjectProvider<MangoJobHandler> handlers,
                                                     MangoNativeJobProperties properties,
                                                     @Value("${spring.application.name:}") String applicationName) {
        return new MangoJobHandlerRegistry(handlers, properties, applicationName);
    }

    @Bean
    @ConditionalOnMissingBean
    MangoJobWorkerExecutor mangoJobWorkerExecutor(IMangoJobHandlerRegistry handlerRegistry) {
        return new MangoJobWorkerExecutor(handlerRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    HttpInternalMangoJobWorkerTransport httpInternalMangoJobWorkerTransport(
            MangoJobWorkerFeignClient workerFeignClient) {
        return new HttpInternalMangoJobWorkerTransport(workerFeignClient);
    }

    @Bean
    @ConditionalOnMissingBean
    MangoJobRemoteWorkerRegistrar mangoJobRemoteWorkerRegistrar(MangoJobFeignClient jobFeignClient,
                                                                IMangoJobHandlerRegistry handlerRegistry,
                                                                MangoNativeJobProperties properties) {
        return new MangoJobRemoteWorkerRegistrar(jobFeignClient, handlerRegistry, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    MangoJobWorkerInternalController mangoJobWorkerInternalController(MangoJobWorkerExecutor workerExecutor) {
        return new MangoJobWorkerInternalController(workerExecutor);
    }
}
