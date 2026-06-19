package io.mango.infra.context.starter;

import io.mango.infra.context.support.MangoContextTaskDecorator;
import io.mango.infra.context.support.TtlAsync;
import io.mango.infra.context.support.TtlExecutorDecorator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Mango 运行时上下文传播自动配置。
 * <p>
 * 提供 {@link TtlAsync}、MangoContext 专用线程池与 {@link TtlExecutorDecorator}。
 *
 * @author Mango
 */
@AutoConfiguration
@EnableAsync
@EnableConfigurationProperties(ContextProperties.class)
@ConditionalOnProperty(prefix = "mango.context", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ContextPropagationAutoConfiguration {

    public static final String MANGO_CONTEXT_EXECUTOR = "mangoContextExecutor";

    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    public TaskDecorator mangoContextTaskDecorator() {
        return new MangoContextTaskDecorator();
    }

    @Bean
    @ConditionalOnMissingBean
    public TtlExecutorDecorator ttlExecutorDecorator() {
        return new TtlExecutorDecorator();
    }

    @Bean(name = MANGO_CONTEXT_EXECUTOR)
    @ConditionalOnMissingBean(name = MANGO_CONTEXT_EXECUTOR)
    @ConditionalOnProperty(prefix = "mango.context.executor", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ThreadPoolTaskExecutor mangoContextExecutor(ContextProperties properties, TaskDecorator taskDecorator) {
        ContextProperties.Executor executorProperties = properties.getExecutor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(executorProperties.getCorePoolSize());
        executor.setMaxPoolSize(executorProperties.getMaxPoolSize());
        executor.setQueueCapacity(executorProperties.getQueueCapacity());
        executor.setKeepAliveSeconds(executorProperties.getKeepAliveSeconds());
        executor.setThreadNamePrefix(executorProperties.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(executorProperties.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(executorProperties.getAwaitTerminationSeconds());
        executor.setTaskDecorator(taskDecorator);
        return executor;
    }
}
