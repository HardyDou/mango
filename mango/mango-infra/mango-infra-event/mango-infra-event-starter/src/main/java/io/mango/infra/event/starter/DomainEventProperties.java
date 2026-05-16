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
     * Reliable publishing backed by KV outbox.
     */
    private Outbox outbox = new Outbox();

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
    }
}
