package io.mango.infra.event.core.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventBus;
import io.mango.infra.event.core.transport.DomainEventTransport;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;

/**
 * Redis Stream based domain event transport.
 */
public class RedisStreamDomainEventTransport implements DomainEventTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisStreamDomainEventTransport.class);
    private static final String EVENT_FIELD = "event";

    private final RStream<String, String> stream;
    private final IDomainEventBus eventBus;
    private final ObjectMapper objectMapper;
    private final String group;
    private final String consumer;
    private final int batchSize;
    private final Duration readTimeout;

    public RedisStreamDomainEventTransport(
            RedissonClient redissonClient,
            IDomainEventBus eventBus,
            ObjectMapper objectMapper,
            String streamName,
            String group,
            String consumer,
            int batchSize,
            Duration readTimeout) {
        Require.notNull(redissonClient, "Redisson 客户端不能为空");
        Require.notNull(eventBus, "事件总线不能为空");
        Require.notNull(objectMapper, "ObjectMapper 不能为空");
        Require.notBlank(streamName, "Redis Stream 名称不能为空");
        Require.notBlank(group, "Redis Stream 消费组不能为空");
        Require.notBlank(consumer, "Redis Stream 消费者不能为空");
        Require.isTrue(batchSize > 0, "Redis Stream 批量大小必须大于 0");
        Require.notNull(readTimeout, "Redis Stream 读取超时不能为空");
        this.stream = redissonClient.getStream(streamName.trim(), StringCodec.INSTANCE);
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
        this.group = group.trim();
        this.consumer = consumer.trim();
        this.batchSize = batchSize;
        this.readTimeout = readTimeout;
        ensureGroup();
    }

    @Override
    public void publish(DomainEvent event) {
        Require.notNull(event, "事件不能为空");
        Require.notBlank(event.getEventType(), "事件类型不能为空");
        stream.add(StreamAddArgs.entry(EVENT_FIELD, write(event)));
    }

    @Override
    public int consumeOnce() {
        Map<StreamMessageId, Map<String, String>> messages = stream.readGroup(
                group,
                consumer,
                StreamReadGroupArgs.neverDelivered()
                        .count(batchSize)
                        .timeout(readTimeout));
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int handled = 0;
        for (Map.Entry<StreamMessageId, Map<String, String>> entry : messages.entrySet()) {
            DomainEvent event = read(entry.getValue().get(EVENT_FIELD), entry.getKey());
            eventBus.publish(event);
            stream.ack(group, entry.getKey());
            handled++;
        }
        return handled;
    }

    private void ensureGroup() {
        try {
            stream.createGroup(StreamCreateGroupArgs.name(group).id(StreamMessageId.NEWEST).makeStream());
        } catch (RedisException ex) {
            if (!isBusyGroup(ex)) {
                throw ex;
            }
            LOGGER.debug("Redis Stream group already exists. group={}", group);
        }
    }

    private boolean isBusyGroup(RedisException ex) {
        String message = ex.getMessage();
        return message != null && message.contains("BUSYGROUP");
    }

    private String write(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private DomainEvent read(String payload, StreamMessageId messageId) {
        Require.notBlank(payload, "Redis Stream 事件载荷不能为空: " + messageId);
        try {
            return objectMapper.readValue(payload, DomainEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to read domain event from Redis Stream: " + messageId, ex);
        }
    }
}
