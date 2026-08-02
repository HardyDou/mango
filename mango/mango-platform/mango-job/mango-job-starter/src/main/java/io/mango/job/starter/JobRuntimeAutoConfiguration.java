package io.mango.job.starter;

import io.mango.job.core.service.nativeengine.IMangoNativeJobRuntime;
import io.mango.job.support.nativeengine.MangoNativeJobProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Runtime-only Job schedulers and worker registration hooks. */
@AutoConfiguration(after = JobAutoConfiguration.class)
@ConditionalOnBean(IMangoNativeJobRuntime.class)
@ConditionalOnProperty(prefix = "mango.bootstrap", name = "mode", havingValue = "runtime",
        matchIfMissing = true)
public class JobRuntimeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MangoNativeJobScheduler mangoNativeJobScheduler(IMangoNativeJobRuntime nativeJobRuntime,
                                                     MangoNativeJobProperties properties) {
        return new MangoNativeJobScheduler(nativeJobRuntime, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    MangoEmbeddedWorkerRegistrar mangoEmbeddedWorkerRegistrar(IMangoNativeJobRuntime nativeJobRuntime,
                                                               MangoNativeJobProperties properties) {
        return new MangoEmbeddedWorkerRegistrar(nativeJobRuntime, properties);
    }
}
