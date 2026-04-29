package io.mango.infra.context.starter;

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

    public Executor getExecutor() {
        return executor;
    }

    /**
     * 线程池配置。
     */
    public static class Executor {

        /**
         * 是否启用平台默认线程池。
         */
        private boolean enabled = true;

        /**
         * 核心线程数。
         */
        private int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors());

        /**
         * 最大线程数。
         */
        private int maxPoolSize = Math.max(16, Runtime.getRuntime().availableProcessors() * 4);

        /**
         * 等待队列容量。
         */
        private int queueCapacity = 1024;

        /**
         * 空闲线程存活秒数。
         */
        private int keepAliveSeconds = 60;

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
        private int awaitTerminationSeconds = 30;

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
