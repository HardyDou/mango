package io.mango.job.starter.remote;

import io.mango.job.support.nativeengine.MangoNativeJobProperties;
import io.mango.job.support.service.MangoJobHandlerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class JobRemoteRuntimeAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JobRemoteRuntimeAutoConfiguration.class))
            .withBean(MangoJobDynamicHttpClient.class, () -> mock(MangoJobDynamicHttpClient.class))
            .withBean(MangoJobHandlerRegistry.class, () -> mock(MangoJobHandlerRegistry.class))
            .withBean(MangoNativeJobProperties.class, MangoNativeJobProperties::new);

    @Test
    void shouldExcludeRemoteWorkerRegistrarFromBootstrapMode() {
        runner.withPropertyValues("mango.bootstrap.mode=bootstrap")
                .run(context -> assertThat(context).doesNotHaveBean(MangoJobRemoteWorkerRegistrar.class));
    }

    @Test
    void shouldRetainRemoteWorkerRegistrarForRuntimeAndLegacyMode() {
        runner.withPropertyValues("mango.bootstrap.mode=runtime")
                .run(context -> assertThat(context).hasSingleBean(MangoJobRemoteWorkerRegistrar.class));
        runner.run(context -> assertThat(context).hasSingleBean(MangoJobRemoteWorkerRegistrar.class));
    }

    @Test
    void shouldNotRunRemoteWorkerHeartbeatBeforeApplicationReady() {
        runner.withPropertyValues("mango.bootstrap.mode=runtime")
                .run(context -> {
                    MangoJobDynamicHttpClient client = context.getBean(MangoJobDynamicHttpClient.class);
                    MangoJobHandlerRegistry registry = context.getBean(MangoJobHandlerRegistry.class);

                    context.getBean(MangoJobRemoteWorkerRegistrar.class).heartbeat();

                    verifyNoInteractions(client, registry);
                });
    }
}
