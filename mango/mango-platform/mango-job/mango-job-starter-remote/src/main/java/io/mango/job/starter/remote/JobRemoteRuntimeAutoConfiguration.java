package io.mango.job.starter.remote;

import io.mango.job.support.nativeengine.MangoNativeJobProperties;
import io.mango.job.support.service.MangoJobHandlerRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Runtime-only remote Job worker registration hooks. */
@AutoConfiguration(after = JobRemoteAutoConfiguration.class)
@ConditionalOnBean({MangoJobDynamicHttpClient.class, MangoJobHandlerRegistry.class})
@ConditionalOnProperty(prefix = "mango.bootstrap", name = "mode", havingValue = "runtime",
        matchIfMissing = true)
public class JobRemoteRuntimeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MangoJobRemoteWorkerRegistrar mangoJobRemoteWorkerRegistrar(MangoJobDynamicHttpClient dynamicHttpClient,
                                                                MangoJobHandlerRegistry handlerRegistry,
                                                                MangoNativeJobProperties properties) {
        return new MangoJobRemoteWorkerRegistrar(dynamicHttpClient, handlerRegistry, properties);
    }
}
