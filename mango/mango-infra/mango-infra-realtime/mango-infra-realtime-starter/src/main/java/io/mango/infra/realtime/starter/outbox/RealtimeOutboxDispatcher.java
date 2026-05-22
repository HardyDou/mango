package io.mango.infra.realtime.starter.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.outbound.IRealtimePublishService;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RealtimeOutboxDispatcher implements AutoCloseable {

    private final IOutboxStore outboxStore;
    private final IRealtimePublishService publishService;
    private final ObjectMapper objectMapper;
    private final String workerId;
    private final int batchSize;
    private final int maxAttempts;
    private final long retryBackoffMillis;
    private final ScheduledExecutorService executor;

    public RealtimeOutboxDispatcher(IOutboxStore outboxStore,
                                    IRealtimePublishService publishService,
                                    ObjectMapper objectMapper,
                                    String workerId,
                                    int batchSize,
                                    int maxAttempts,
                                    long retryBackoffMillis,
                                    long initialDelayMillis,
                                    long fixedDelayMillis) {
        this.outboxStore = outboxStore;
        this.publishService = publishService;
        this.objectMapper = objectMapper;
        this.workerId = workerId == null || workerId.isBlank() ? "realtime-outbox-worker" : workerId.trim();
        this.batchSize = batchSize <= 0 ? 50 : batchSize;
        this.maxAttempts = maxAttempts <= 0 ? 5 : maxAttempts;
        this.retryBackoffMillis = retryBackoffMillis <= 0 ? 1000L : retryBackoffMillis;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "realtime-outbox-dispatcher-" + this.workerId);
            thread.setDaemon(true);
            return thread;
        });
        long safeInitialDelayMillis = Math.max(0L, initialDelayMillis);
        long safeFixedDelayMillis = fixedDelayMillis <= 0 ? 500L : fixedDelayMillis;
        this.executor.scheduleWithFixedDelay(this::dispatchReadyMessages,
                safeInitialDelayMillis,
                safeFixedDelayMillis,
                TimeUnit.MILLISECONDS);
    }

    public void dispatchReadyMessages() {
        Instant now = Instant.now();
        List<OutboxMessage> messages = outboxStore.claim(workerId, RealtimeOutboxPublisher.EVENT_TYPE, batchSize, now);
        for (OutboxMessage message : messages) {
            dispatch(message);
        }
    }

    private void dispatch(OutboxMessage outboxMessage) {
        try {
            RealtimeOutboundMessage message = readPayload(outboxMessage);
            publishService.publish(message);
            outboxStore.ack(outboxMessage.getMessageId(), workerId, Instant.now());
        } catch (Exception e) {
            if (outboxMessage.getAttemptCount() >= maxAttempts) {
                log.warn("Realtime outbox message {} reached max attempts {}, retrying with backoff",
                        outboxMessage.getMessageId(), maxAttempts, e);
            } else {
                log.warn("Failed to dispatch realtime outbox message {}", outboxMessage.getMessageId(), e);
            }
            outboxStore.nack(
                    outboxMessage.getMessageId(),
                    workerId,
                    e.getMessage(),
                    Instant.now().plus(retryDelay(outboxMessage.getAttemptCount())),
                    Instant.now());
        }
    }

    private RealtimeOutboundMessage readPayload(OutboxMessage outboxMessage) {
        RealtimeOutboxPayload payload = objectMapper.convertValue(outboxMessage.getPayload(), RealtimeOutboxPayload.class);
        if (payload.message() == null) {
            throw new IllegalArgumentException("Realtime outbox payload message is required");
        }
        return payload.message();
    }

    private Duration retryDelay(int attemptCount) {
        long multiplier = Math.max(1, Math.min(attemptCount, 10));
        return Duration.ofMillis(retryBackoffMillis * multiplier);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
