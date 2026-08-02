package io.mango.job.starter;

import io.mango.job.core.service.nativeengine.IMangoNativeJobRuntime;
import io.mango.job.support.nativeengine.MangoNativeJobProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class JobRuntimeAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JobRuntimeAutoConfiguration.class))
            .withBean(IMangoNativeJobRuntime.class, () -> mock(IMangoNativeJobRuntime.class))
            .withBean(MangoNativeJobProperties.class, MangoNativeJobProperties::new);

    @Test
    void shouldExcludeSchedulersAndRegistrarsFromBootstrapMode() {
        runner.withPropertyValues("mango.bootstrap.mode=bootstrap")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MangoNativeJobScheduler.class);
                    assertThat(context).doesNotHaveBean(MangoEmbeddedWorkerRegistrar.class);
                });
    }

    @Test
    void shouldRetainRuntimeWorkersForRuntimeAndLegacyMode() {
        runner.withPropertyValues("mango.bootstrap.mode=runtime")
                .run(context -> {
                    assertThat(context).hasSingleBean(MangoNativeJobScheduler.class);
                    assertThat(context).hasSingleBean(MangoEmbeddedWorkerRegistrar.class);
                });
        runner.run(context -> {
            assertThat(context).hasSingleBean(MangoNativeJobScheduler.class);
            assertThat(context).hasSingleBean(MangoEmbeddedWorkerRegistrar.class);
        });
    }

    @Test
    void shouldNotRunSchedulersOrWorkerHeartbeatsBeforeApplicationReady() {
        runner.withPropertyValues("mango.bootstrap.mode=runtime")
                .run(context -> {
                    IMangoNativeJobRuntime runtime = context.getBean(IMangoNativeJobRuntime.class);
                    context.getBean(MangoNativeJobScheduler.class).tick();
                    context.getBean(MangoEmbeddedWorkerRegistrar.class).heartbeat();

                    verifyNoInteractions(runtime);
                });
    }
}
