package io.mango.infra.context.starter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mango 运行时上下文传播配置属性。
 * <p>
 * 当前保留用于后续扩展自动包装策略。
 *
 * @author Mango
 */
@ConfigurationProperties(prefix = "mango.context")
public class ContextProperties {

    /**
     * 平台默认异步线程池配置。
     */
    private final Executor executor = new Executor();

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding intentionally exposes this nested property bean")
    public Executor getExecutor() {
        return executor;
    }

    /**
     * 线程池配置。
     */
    public static class Executor {

        private static final int DEFAULT_CORE_POOL_SIZE_MIN = 2;
        private static final int DEFAULT_MAX_POOL_SIZE_MIN = 16;
        private static final int DEFAULT_MAX_POOL_SIZE_MULTIPLIER = 4;
        private static final int DEFAULT_QUEUE_CAPACITY = 1024;
        private static final int DEFAULT_KEEP_ALIVE_SECONDS = 60;
        private static final int DEFAULT_AWAIT_TERMINATION_SECONDS = 30;

        /**
         * 是否启用平台默认线程池。
         */
        private boolean enabled = true;

        /**
         * 核心线程数。
         */
        private int corePoolSize = Math.max(DEFAULT_CORE_POOL_SIZE_MIN, Runtime.getRuntime().availableProcessors());

        /**
         * 最大线程数。
         */
        private int maxPoolSize = Math.max(
                DEFAULT_MAX_POOL_SIZE_MIN,
                Runtime.getRuntime().availableProcessors() * DEFAULT_MAX_POOL_SIZE_MULTIPLIER);

        /**
         * 等待队列容量。
         */
        private int queueCapacity = DEFAULT_QUEUE_CAPACITY;

        /**
         * 空闲线程存活秒数。
         */
        private int keepAliveSeconds = DEFAULT_KEEP_ALIVE_SECONDS;

        /**
         * 线程名前缀。
         */
        private String threadNamePrefix = "mango-context-async-";

        /**
         * 停机时是否等待任务完成。
         */
        private boolean waitForTasksToCompleteOnShutdown = true;

        /**
         * 停机等待秒数。
         */
        private int awaitTerminationSeconds = DEFAULT_AWAIT_TERMINATION_SECONDS;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public int getKeepAliveSeconds() {
            return keepAliveSeconds;
        }

        public void setKeepAliveSeconds(int keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        public boolean isWaitForTasksToCompleteOnShutdown() {
            return waitForTasksToCompleteOnShutdown;
        }

        public void setWaitForTasksToCompleteOnShutdown(boolean waitForTasksToCompleteOnShutdown) {
            this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
        }

        public int getAwaitTerminationSeconds() {
            return awaitTerminationSeconds;
        }

        public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
            this.awaitTerminationSeconds = awaitTerminationSeconds;
        }
    }
}
