package io.mango.infra.event.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 领域事件配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.event")
public class DomainEventProperties {

    /**
     * 事件总线类型。当前实现 memory，后续扩展 redis/db。
     */
    private String type = "memory";

    /**
     * Cross-process transport. none or redis-stream.
     */
    private String transport = "none";

    /**
     * Reliable publishing backed by KV outbox.
     */
    private Outbox outbox = new Outbox();

    /**
     * Redis Stream transport options.
     */
    private RedisStream redisStream = new RedisStream();

    @Data
    public static class Outbox {

        /**
         * Whether IDomainEventPublisher writes events to KV outbox.
         */
        private boolean enabled;

        /**
         * Worker id used when claiming outbox messages.
         */
        private String workerId = "domain-event-dispatcher";

        /**
         * Claim batch size per dispatch.
         */
        private int batchSize = 50;

        /**
         * Retry delay after dispatch failure.
         */
        private long retryDelaySeconds = 60L;

        /**
         * Max attempts before an outbox message becomes final failed.
         */
        private int maxAttempts = 5;

        /**
         * Whether the starter runs an in-process dispatch scheduler.
         */
        private boolean dispatchEnabled = true;

        /**
         * Fixed delay between dispatch attempts.
         */
        private long dispatchIntervalMillis = 1000L;

        /**
         * Initial delay before the first dispatch attempt.
         */
        private long dispatchInitialDelayMillis = 1000L;
    }

    @Data
    public static class RedisStream {

        /**
         * Redis Stream key name.
         */
        private String streamName = "mango:domain-event";

        /**
         * Redis consumer group.
         */
        private String group = "mango-domain-event";

        /**
         * Redis consumer name.
         */
        private String consumer = "domain-event-consumer";

        /**
         * Batch size per consume.
         */
        private int batchSize = 50;

        /**
         * Read timeout in milliseconds.
         */
        private long readTimeoutMillis = 200L;

        /**
         * Whether the starter runs an in-process transport consumer.
         */
        private boolean consumeEnabled = true;

        /**
         * Fixed delay between consume attempts.
         */
        private long consumeIntervalMillis = 1000L;

        /**
         * Initial delay before the first consume attempt.
         */
        private long consumeInitialDelayMillis = 1000L;
    }
}
