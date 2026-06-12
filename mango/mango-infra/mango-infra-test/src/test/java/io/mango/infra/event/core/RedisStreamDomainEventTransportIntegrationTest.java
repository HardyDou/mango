package io.mango.infra.event.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.core.memory.InMemoryDomainEventBus;
import io.mango.infra.event.core.redis.RedisStreamDomainEventTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.config.Config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real Redis Stream integration test for domain event transport.
 * Requires Redis running on localhost:6379 with no password.
 */
class RedisStreamDomainEventTransportIntegrationTest {

    private static final String KEY_PREFIX = "mango:test:domain-event:" + System.currentTimeMillis();
    private static final String STREAM_NAME = KEY_PREFIX + ":stream";
    private static final String GROUP = KEY_PREFIX + ":group";
    private static final String CONSUMER = KEY_PREFIX + ":consumer";

    private static RedissonClient redisson;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setDatabase(0)
                .setConnectTimeout(5000)
                .setTimeout(3000)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(8);
        redisson = Redisson.create(config);
        objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @AfterAll
    static void afterAll() {
        if (redisson != null) {
            redisson.getKeys().deleteByPattern(KEY_PREFIX + "*");
            redisson.shutdown();
        }
    }

    @BeforeEach
    void setUp() {
        redisson.getKeys().deleteByPattern(KEY_PREFIX + "*");
    }

    @Test
    void publishAndConsumeOnce_shouldDeliverThroughRealRedisStreamAndAck() {
        InMemoryDomainEventBus eventBus = new InMemoryDomainEventBus();
        List<DomainEvent> handled = new ArrayList<>();
        eventBus.subscribe("issue137.real.redis-stream", handled::add);

        RedisStreamDomainEventTransport transport = new RedisStreamDomainEventTransport(
                redisson,
                eventBus,
                objectMapper,
                STREAM_NAME,
                GROUP,
                CONSUMER,
                10,
                Duration.ofMillis(100));

        DomainEvent event = DomainEvent.builder()
                .eventType("issue137.real.redis-stream")
                .businessType("ISSUE")
                .businessKey("ISSUE-137-REDIS-STREAM")
                .aggregateId("137")
                .payload("source", "real-redis")
                .header("tenantId", "1")
                .build();

        transport.publish(event);

        RStream<String, String> stream = redisson.getStream(STREAM_NAME);
        assertThat(stream.size()).isEqualTo(1);

        int consumed = transport.consumeOnce();

        assertThat(consumed).isEqualTo(1);
        assertThat(handled).hasSize(1);
        assertThat(handled.get(0).getEventId()).isEqualTo(event.getEventId());
        assertThat(handled.get(0).getBusinessKey()).isEqualTo("ISSUE-137-REDIS-STREAM");
        assertThat(handled.get(0).getPayload()).containsEntry("source", "real-redis");
        assertThat(handled.get(0).getHeaders()).containsEntry("tenantId", "1");
        assertThat(stream.listPending(GROUP, StreamMessageId.MIN, StreamMessageId.MAX, 10)).isEmpty();
    }
}
