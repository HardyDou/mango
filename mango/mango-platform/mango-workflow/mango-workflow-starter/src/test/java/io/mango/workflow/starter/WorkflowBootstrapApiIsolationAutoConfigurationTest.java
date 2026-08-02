package io.mango.workflow.starter;

import io.mango.workflow.api.WorkflowTaskRuntimeApi;
import io.mango.workflow.starter.controller.WorkflowTaskController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WorkflowBootstrapApiIsolationAutoConfigurationTest {

    @Test
    void bootstrapApiInjection_defersControllerCreationUntilApiInvocation() {
        AtomicInteger controllerCreations = new AtomicInteger();
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        WorkflowBootstrapApiIsolationAutoConfiguration.class))
                .withPropertyValues("mango.bootstrap.mode=bootstrap")
                .withBean(WorkflowTaskController.class, () -> {
                    controllerCreations.incrementAndGet();
                    return mock(WorkflowTaskController.class);
                }, definition -> definition.setLazyInit(true))
                .withBean(ApiConsumer.class);

        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ApiConsumer.class);
            assertThat(controllerCreations).hasValue(0);

            context.getBean(ApiConsumer.class).api().summary();

            assertThat(controllerCreations).hasValue(1);
        });
    }

    @Test
    void runtimeMode_doesNotRegisterBootstrapApiProxy() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        WorkflowBootstrapApiIsolationAutoConfiguration.class))
                .withPropertyValues("mango.bootstrap.mode=runtime")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(WorkflowTaskRuntimeApi.class);
                });
    }

    @Test
    void disabledWorkflow_doesNotRegisterBootstrapApiProxy() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        WorkflowBootstrapApiIsolationAutoConfiguration.class))
                .withPropertyValues(
                        "mango.bootstrap.mode=bootstrap",
                        "mango.workflow.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(WorkflowTaskRuntimeApi.class);
                });
    }

    private record ApiConsumer(WorkflowTaskRuntimeApi api) {
    }
}
