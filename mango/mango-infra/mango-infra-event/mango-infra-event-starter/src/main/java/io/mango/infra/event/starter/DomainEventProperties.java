package io.mango.infra.event.starter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding intentionally exposes this nested property bean")
    public Outbox getOutbox() {
        return outbox;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding intentionally stores this nested property bean")
    public void setOutbox(Outbox outbox) {
        this.outbox = outbox;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding intentionally exposes this nested property bean")
    public RedisStream getRedisStream() {
        return redisStream;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration binding intentionally stores this nested property bean")
    public void setRedisStream(RedisStream redisStream) {
        this.redisStream = redisStream;
    }

    @Data
    public static class Outbox {

        private static final int DEFAULT_BATCH_SIZE = 50;
        private static final long DEFAULT_RETRY_DELAY_SECONDS = 60L;
        private static final int DEFAULT_MAX_ATTEMPTS = 5;
        private static final long DEFAULT_DISPATCH_INTERVAL_MILLIS = 1000L;
        private static final long DEFAULT_DISPATCH_INITIAL_DELAY_MILLIS = 1000L;

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
        private int batchSize = DEFAULT_BATCH_SIZE;

        /**
         * Retry delay after dispatch failure.
         */
        private long retryDelaySeconds = DEFAULT_RETRY_DELAY_SECONDS;

        /**
         * Max attempts before an outbox message becomes final failed.
         */
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

        /**
         * Whether the starter runs an in-process dispatch scheduler.
         */
        private boolean dispatchEnabled = true;

        /**
         * Fixed delay between dispatch attempts.
         */
        private long dispatchIntervalMillis = DEFAULT_DISPATCH_INTERVAL_MILLIS;

        /**
         * Initial delay before the first dispatch attempt.
         */
        private long dispatchInitialDelayMillis = DEFAULT_DISPATCH_INITIAL_DELAY_MILLIS;
    }

    @Data
    public static class RedisStream {

        private static final int DEFAULT_BATCH_SIZE = 50;
        private static final long DEFAULT_READ_TIMEOUT_MILLIS = 200L;
        private static final long DEFAULT_PENDING_IDLE_TIMEOUT_MILLIS = 60000L;
        private static final long DEFAULT_CONSUME_INTERVAL_MILLIS = 1000L;
        private static final long DEFAULT_CONSUME_INITIAL_DELAY_MILLIS = 1000L;

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
        private int batchSize = DEFAULT_BATCH_SIZE;

        /**
         * Read timeout in milliseconds.
         */
        private long readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS;

        /**
         * Idle time before pending messages can be claimed by another consumer.
         */
        private long pendingIdleTimeoutMillis = DEFAULT_PENDING_IDLE_TIMEOUT_MILLIS;

        /**
         * Whether the starter runs an in-process transport consumer.
         */
        private boolean consumeEnabled = true;

        /**
         * Fixed delay between consume attempts.
         */
        private long consumeIntervalMillis = DEFAULT_CONSUME_INTERVAL_MILLIS;

        /**
         * Initial delay before the first consume attempt.
         */
        private long consumeInitialDelayMillis = DEFAULT_CONSUME_INITIAL_DELAY_MILLIS;
    }
}
