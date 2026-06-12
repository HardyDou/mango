package io.mango.infra.event.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.event.core.memory.InMemoryDomainEventBus;
import io.mango.infra.event.core.outbox.OutboxDomainEventPublisher;
import io.mango.infra.event.core.redis.RedisStreamDomainEventTransport;
import io.mango.infra.event.core.transport.TransportDomainEventDispatcher;
import io.mango.infra.kv.api.IOutboxDispatcher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxStatus;
import io.mango.infra.kv.core.outbox.KvOutboxPublisher;
import io.mango.infra.kv.core.outbox.KvOutboxStore;
import io.mango.infra.kv.core.redis.RedisKvStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.config.Config;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real Redis Stream integration test for domain event transport.
 * Requires Redis running on localhost:6379 with no password.
 */
class RedisStreamDomainEventTransportIntegrationTest {

    private static final String KEY_PREFIX = "mango:test:domain-event:" + System.currentTimeMillis();
    private static final String STREAM_NAME = KEY_PREFIX + ":stream";
    private static final String GROUP = KEY_PREFIX + ":group";
    private static final String CONSUMER = KEY_PREFIX + ":consumer";
    private static final Duration READ_TIMEOUT = Duration.ofMillis(100);
    private static final Duration PENDING_IDLE_TIMEOUT = Duration.ofMillis(1);

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
                READ_TIMEOUT,
                PENDING_IDLE_TIMEOUT);

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

    @Test
    void outboxRelay_shouldPublishToRealRedisStreamAndBroadcastToServiceGroups() {
        IOutboxStore outboxStore = new KvOutboxStore(new RedisKvStore(redisson), objectMapper);
        IDomainEventPublisher publisher = new OutboxDomainEventPublisher(new KvOutboxPublisher(outboxStore));

        InMemoryDomainEventBus paymentBus = new InMemoryDomainEventBus();
        InMemoryDomainEventBus noticeBus = new InMemoryDomainEventBus();
        List<DomainEvent> paymentHandled = new ArrayList<>();
        List<DomainEvent> noticeHandled = new ArrayList<>();
        paymentBus.subscribe("workflow.process.completed", paymentHandled::add);
        noticeBus.subscribe("workflow.process.completed", noticeHandled::add);

        RedisStreamDomainEventTransport relayTransport = transport(new InMemoryDomainEventBus(), "relay", "publisher");
        IOutboxDispatcher dispatcher = new TransportDomainEventDispatcher(
                outboxStore,
                relayTransport,
                Clock.systemUTC(),
                "issue-137-relay",
                10,
                0,
                3);
        RedisStreamDomainEventTransport paymentTransport = transport(paymentBus, "payment-service", "payment-instance-a");
        RedisStreamDomainEventTransport noticeTransport = transport(noticeBus, "notice-service", "notice-instance-a");

        DomainEvent event = DomainEvent.builder()
                .eventType("workflow.process.completed")
                .businessType("PAYMENT_REFUND_APPROVAL")
                .businessKey("REFUND-ISSUE-137")
                .aggregateId("workflow-137")
                .payload("approved", true)
                .header("tenantId", "1")
                .build();

        publisher.publish(event);

        assertThat(dispatcher.dispatchOnce()).isEqualTo(1);
        assertThat(outboxStore.findById(event.getEventId()).getStatus()).isEqualTo(OutboxStatus.SUCCESS);
        assertThat(redisson.getStream(STREAM_NAME).size()).isEqualTo(1);

        assertThat(paymentTransport.consumeOnce()).isEqualTo(1);
        assertThat(noticeTransport.consumeOnce()).isEqualTo(1);

        assertThat(paymentHandled).hasSize(1);
        assertThat(noticeHandled).hasSize(1);
        assertThat(paymentHandled.get(0).getEventId()).isEqualTo(event.getEventId());
        assertThat(noticeHandled.get(0).getEventId()).isEqualTo(event.getEventId());
        assertThat(paymentHandled.get(0).getBusinessType()).isEqualTo("PAYMENT_REFUND_APPROVAL");
        assertThat(noticeHandled.get(0).getBusinessKey()).isEqualTo("REFUND-ISSUE-137");
    }

    @Test
    void sameServiceGroup_shouldCompeteAndDeliverOneEventOnlyOnce() {
        InMemoryDomainEventBus firstBus = new InMemoryDomainEventBus();
        InMemoryDomainEventBus secondBus = new InMemoryDomainEventBus();
        List<DomainEvent> firstHandled = new ArrayList<>();
        List<DomainEvent> secondHandled = new ArrayList<>();
        firstBus.subscribe("issue137.same-group", firstHandled::add);
        secondBus.subscribe("issue137.same-group", secondHandled::add);

        RedisStreamDomainEventTransport firstConsumer = transport(firstBus, "payment-service", "payment-instance-a");
        RedisStreamDomainEventTransport secondConsumer = transport(secondBus, "payment-service", "payment-instance-b");

        DomainEvent event = DomainEvent.builder()
                .eventType("issue137.same-group")
                .businessType("ISSUE")
                .businessKey("ISSUE-137-SAME-GROUP")
                .aggregateId("137")
                .build();
        transport(new InMemoryDomainEventBus(), "relay", "publisher").publish(event);

        int firstCount = firstConsumer.consumeOnce();
        int secondCount = secondConsumer.consumeOnce();

        assertThat(firstCount + secondCount).isEqualTo(1);
        assertThat(firstHandled.size() + secondHandled.size()).isEqualTo(1);
        assertThat(redisson.getStream(STREAM_NAME)
                .listPending(groupName("payment-service"), StreamMessageId.MIN, StreamMessageId.MAX, 10)).isEmpty();
    }

    @Test
    void consumeOnce_shouldClaimIdlePendingMessageAndAckAfterRecovery() {
        InMemoryDomainEventBus failingBus = new InMemoryDomainEventBus();
        failingBus.subscribe("issue137.pending-recovery", ignored -> {
            throw new IllegalStateException("consumer crashed before ack");
        });
        RedisStreamDomainEventTransport failingConsumer = transport(failingBus, "payment-service", "payment-instance-a");

        DomainEvent event = DomainEvent.builder()
                .eventType("issue137.pending-recovery")
                .businessType("ISSUE")
                .businessKey("ISSUE-137-PENDING")
                .aggregateId("137")
                .build();
        transport(new InMemoryDomainEventBus(), "relay", "publisher").publish(event);

        assertThatThrownBy(failingConsumer::consumeOnce)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("consumer crashed before ack");
        assertThat(redisson.getStream(STREAM_NAME)
                .listPending(groupName("payment-service"), StreamMessageId.MIN, StreamMessageId.MAX, 10)).hasSize(1);

        InMemoryDomainEventBus recoveredBus = new InMemoryDomainEventBus();
        List<DomainEvent> recovered = new ArrayList<>();
        recoveredBus.subscribe("issue137.pending-recovery", recovered::add);
        RedisStreamDomainEventTransport recoveredConsumer = transport(recoveredBus, "payment-service", "payment-instance-b");

        awaitPendingIdleTimeout();
        assertThat(recoveredConsumer.consumeOnce()).isEqualTo(1);
        assertThat(recovered).hasSize(1);
        assertThat(recovered.get(0).getEventId()).isEqualTo(event.getEventId());
        assertThat(redisson.getStream(STREAM_NAME)
                .listPending(groupName("payment-service"), StreamMessageId.MIN, StreamMessageId.MAX, 10)).isEmpty();
    }

    private RedisStreamDomainEventTransport transport(InMemoryDomainEventBus eventBus, String group, String consumer) {
        return new RedisStreamDomainEventTransport(
                redisson,
                eventBus,
                objectMapper,
                STREAM_NAME,
                KEY_PREFIX + ":" + group,
                KEY_PREFIX + ":" + consumer,
                10,
                READ_TIMEOUT,
                PENDING_IDLE_TIMEOUT);
    }

    private String groupName(String group) {
        return KEY_PREFIX + ":" + group;
    }

    private void awaitPendingIdleTimeout() {
        try {
            Thread.sleep(PENDING_IDLE_TIMEOUT.plusMillis(5).toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Redis pending idle timeout", ex);
        }
    }
}
