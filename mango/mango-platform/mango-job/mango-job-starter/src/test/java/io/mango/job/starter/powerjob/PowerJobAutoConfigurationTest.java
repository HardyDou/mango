package io.mango.job.starter.powerjob;

import io.mango.job.core.service.engine.IMangoJobEngine;
import io.mango.job.core.service.IMangoJobHandlerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.worker.PowerJobSpringWorker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PowerJobAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PowerJobAutoConfiguration.class));

    @Test
    void shouldNotCreateAdapterWhenPowerJobDisabledByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(IMangoJobEngine.class));
    }

    @Test
    void shouldCreateAdapterWhenPowerJobEnabled() {
        runner.withUserConfiguration(TestBeans.class)
                .withPropertyValues(
                        "mango.job.powerjob.enabled=true",
                        "mango.job.powerjob.app-id=10001")
                .run(context -> {
                    assertThat(context).hasSingleBean(IMangoJobEngine.class);
                    assertThat(context.getBean(IMangoJobEngine.class).engineType()).isEqualTo("POWERJOB");
                });
    }

    @Test
    void shouldFailFastWhenPowerJobEnabledWithoutClientProperties() {
        runner.withPropertyValues("mango.job.powerjob.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("mango.job.powerjob.server-addresses 不能为空");
                });
    }

    @Test
    void shouldCreateLazyOperationsWhenPowerJobClientPropertiesComplete() {
        runner.withPropertyValues(
                        "mango.job.powerjob.enabled=true",
                        "mango.job.powerjob.server-addresses=127.0.0.1:7700",
                        "mango.job.powerjob.app-name=mango-job",
                        "mango.job.powerjob.password=secret",
                        "mango.job.powerjob.app-id=10001")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PowerJobClient.class);
                    assertThat(context).hasSingleBean(IPowerJobClientOperations.class);
                    assertThat(context).hasSingleBean(IMangoJobEngine.class);
                });
    }

    @Test
    void shouldFailFastWhenClientServerAddressContainsProtocol() {
        runner.withPropertyValues(
                        "mango.job.powerjob.enabled=true",
                        "mango.job.powerjob.server-addresses=http://127.0.0.1:7700",
                        "mango.job.powerjob.app-name=mango-job",
                        "mango.job.powerjob.password=secret",
                        "mango.job.powerjob.app-id=10001")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "PowerJob server-addresses 使用 host:port 格式，不要携带 http(s)://：http://127.0.0.1:7700");
                });
    }

    @Test
    void shouldNotCreateWorkerWhenWorkerDisabledByDefault() {
        runner.withUserConfiguration(TestBeans.class)
                .withPropertyValues(
                        "mango.job.powerjob.enabled=true",
                        "mango.job.powerjob.app-id=10001")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PowerJobSpringWorker.class);
                    assertThat(context).doesNotHaveBean(MangoPowerJobProcessor.class);
                });
    }

    @Test
    void shouldCreateWorkerWhenWorkerEnabled() {
        runner.withUserConfiguration(TestBeans.class)
                .withPropertyValues(
                        "mango.job.powerjob.enabled=true",
                        "mango.job.powerjob.app-id=10001",
                        "mango.job.powerjob.app-name=mango-job",
                        "mango.job.powerjob.server-addresses=127.0.0.1:7700",
                        "mango.job.powerjob.worker.enabled=true",
                        "mango.job.powerjob.worker.port=27777")
                .run(context -> {
                    assertThat(context).hasSingleBean(PowerJobSpringWorker.class);
                    assertThat(context).hasSingleBean(MangoPowerJobProcessor.class);
                });
    }

    @Test
    void shouldFailFastWhenWorkerEnabledWithoutServerAddress() {
        runner.withUserConfiguration(TestBeans.class)
                .withPropertyValues(
                        "mango.job.powerjob.enabled=true",
                        "mango.job.powerjob.app-id=10001",
                        "mango.job.powerjob.app-name=mango-job",
                        "mango.job.powerjob.worker.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("mango.job.powerjob.worker.server-addresses 或 mango.job.powerjob.server-addresses 不能为空");
                });
    }

    @Test
    void shouldFailFastWhenWorkerServerAddressContainsProtocol() {
        runner.withUserConfiguration(TestBeans.class)
                .withPropertyValues(
                        "mango.job.powerjob.enabled=true",
                        "mango.job.powerjob.app-id=10001",
                        "mango.job.powerjob.app-name=mango-job",
                        "mango.job.powerjob.server-addresses=http://127.0.0.1:7700",
                        "mango.job.powerjob.worker.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "PowerJob server-addresses 使用 host:port 格式，不要携带 http(s)://：http://127.0.0.1:7700");
                });
    }

    @Configuration
    static class TestBeans {

        @Bean
        PowerJobClient powerJobClient() {
            return mock(PowerJobClient.class);
        }

        @Bean
        IPowerJobClientOperations powerJobClientOperations() {
            return mock(IPowerJobClientOperations.class);
        }

        @Bean
        IMangoJobHandlerRegistry mangoJobHandlerRegistry() {
            return mock(IMangoJobHandlerRegistry.class);
        }
    }
}
