package io.mango.infra.context.starter;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.context.support.MangoContextTaskDecorator;
import io.mango.infra.context.support.TtlExecutorDecorator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextPropagationAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ContextPropagationAutoConfiguration.class));

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void defaults_registerPropertiesDecoratorsAndNamedExecutor() {
        runner.run(context -> {
            assertTrue(context.containsBean(ContextPropagationAutoConfiguration.MANGO_CONTEXT_EXECUTOR));
            assertTrue(context.getBean(TaskDecorator.class) instanceof MangoContextTaskDecorator);
            assertTrue(context.getBean(TtlExecutorDecorator.class) instanceof TtlExecutorDecorator);
            assertTrue(context.getBean(ContextProperties.class).getExecutor().isEnabled());
        });
    }

    @Test
    void disabledMainSwitch_registersNoContextBeans() {
        runner.withPropertyValues("mango.context.enabled=false")
                .run(context -> {
                    assertEquals(0, context.getBeansOfType(ContextProperties.class).size());
                    assertEquals(0, context.getBeansOfType(TaskDecorator.class).size());
                    assertEquals(0, context.getBeansOfType(TtlExecutorDecorator.class).size());
                    assertTrue(!context.containsBean(ContextPropagationAutoConfiguration.MANGO_CONTEXT_EXECUTOR));
                });
    }

    @Test
    void disabledExecutor_keepsDecoratorsButDoesNotRegisterDefaultExecutor() {
        runner.withPropertyValues("mango.context.executor.enabled=false")
                .run(context -> {
                    assertTrue(context.getBean(TaskDecorator.class) instanceof MangoContextTaskDecorator);
                    assertTrue(context.getBean(TtlExecutorDecorator.class) instanceof TtlExecutorDecorator);
                    assertTrue(!context.containsBean(ContextPropagationAutoConfiguration.MANGO_CONTEXT_EXECUTOR));
                });
    }

    @Test
    void userBeans_makeDefaultsBackOff() {
        runner.withUserConfiguration(UserBeans.class)
                .run(context -> {
                    assertSame(UserBeans.TASK_DECORATOR, context.getBean(TaskDecorator.class));
                    assertSame(UserBeans.EXECUTOR,
                            context.getBean(ContextPropagationAutoConfiguration.MANGO_CONTEXT_EXECUTOR));
                });
    }

    @Test
    void properties_bindEveryExecutorSetting() {
        runner.withPropertyValues(
                        "mango.context.executor.enabled=true",
                        "mango.context.executor.core-pool-size=1",
                        "mango.context.executor.max-pool-size=2",
                        "mango.context.executor.queue-capacity=3",
                        "mango.context.executor.keep-alive-seconds=4",
                        "mango.context.executor.thread-name-prefix=test-context-",
                        "mango.context.executor.wait-for-tasks-to-complete-on-shutdown=false",
                        "mango.context.executor.await-termination-seconds=5")
                .run(context -> {
                    ContextProperties.Executor properties = context.getBean(ContextProperties.class).getExecutor();
                    assertEquals(1, properties.getCorePoolSize());
                    assertEquals(2, properties.getMaxPoolSize());
                    assertEquals(3, properties.getQueueCapacity());
                    assertEquals(4, properties.getKeepAliveSeconds());
                    assertEquals("test-context-", properties.getThreadNamePrefix());
                    assertTrue(!properties.isWaitForTasksToCompleteOnShutdown());
                    assertEquals(5, properties.getAwaitTerminationSeconds());
                });
    }

    @Test
    void actualNamedExecutor_propagatesContextAndUsesConfiguredThreadPrefix() {
        runner.withPropertyValues(
                        "mango.context.executor.core-pool-size=1",
                        "mango.context.executor.max-pool-size=1",
                        "mango.context.executor.thread-name-prefix=e2e-context-")
                .run(context -> {
                    ThreadPoolTaskExecutor executor = context.getBean(
                            ContextPropagationAutoConfiguration.MANGO_CONTEXT_EXECUTOR,
                            ThreadPoolTaskExecutor.class);
                    MangoContextHolder.set(MangoContextSnapshot.request(
                            "request-1", null, "tenant-1", null, null));
                    MangoContextHolder.setToken("token-1");

                    String observed = executor.submit(() -> String.join(":",
                                    MangoContextHolder.requestId(),
                                    MangoContextHolder.tenantId(),
                                    MangoContextHolder.token(),
                                    Thread.currentThread().getName()))
                            .get(2, TimeUnit.SECONDS);

                    assertEquals("request-1:tenant-1:token-1:e2e-context-1", observed);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class UserBeans {

        private static final TaskDecorator TASK_DECORATOR = runnable -> runnable;
        private static final ThreadPoolTaskExecutor EXECUTOR = new ThreadPoolTaskExecutor();

        @Bean
        TaskDecorator taskDecorator() {
            return TASK_DECORATOR;
        }

        @Bean(name = ContextPropagationAutoConfiguration.MANGO_CONTEXT_EXECUTOR)
        ThreadPoolTaskExecutor mangoContextExecutor() {
            return EXECUTOR;
        }
    }
}
