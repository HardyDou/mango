package io.mango.infra.realtime.starter.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxTopics;
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

    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final int MAX_RETRY_MULTIPLIER = 10;
    private static final long DEFAULT_RETRY_BACKOFF_MILLIS = 1_000L;
    private static final long DEFAULT_FIXED_DELAY_MILLIS = 500L;

    private final IOutboxStore outboxStore;
    private final IRealtimePublishService publishService;
    private final ObjectMapper objectMapper;
    private final String workerId;
    private final int batchSize;
    private final int maxAttempts;
    private final long retryBackoffMillis;
    private final ScheduledExecutorService executor;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Outbox, publisher and ObjectMapper are injected singleton collaborators")
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
        this.workerId = defaultWorkerId(workerId);
        this.batchSize = positiveOrDefault(batchSize, DEFAULT_BATCH_SIZE);
        this.maxAttempts = positiveOrDefault(maxAttempts, DEFAULT_MAX_ATTEMPTS);
        this.retryBackoffMillis = positiveOrDefault(retryBackoffMillis, DEFAULT_RETRY_BACKOFF_MILLIS);
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "realtime-outbox-dispatcher-" + this.workerId);
            thread.setDaemon(true);
            return thread;
        });
        long safeInitialDelayMillis = Math.max(0L, initialDelayMillis);
        long safeFixedDelayMillis = positiveOrDefault(fixedDelayMillis, DEFAULT_FIXED_DELAY_MILLIS);
        this.executor.scheduleWithFixedDelay(this::dispatchReadyMessages,
                safeInitialDelayMillis,
                safeFixedDelayMillis,
                TimeUnit.MILLISECONDS);
    }

    public void dispatchReadyMessages() {
        Instant now = Instant.now();
        List<OutboxMessage> messages = outboxStore.claimByTopic(workerId, OutboxTopics.REALTIME, batchSize, now);
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
        long multiplier = Math.max(1, Math.min(attemptCount, MAX_RETRY_MULTIPLIER));
        return Duration.ofMillis(retryBackoffMillis * multiplier);
    }

    private String defaultWorkerId(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return "realtime-outbox-worker";
        }
        return candidate.trim();
    }

    private int positiveOrDefault(int candidate, int defaultValue) {
        if (candidate <= 0) {
            return defaultValue;
        }
        return candidate;
    }

    private long positiveOrDefault(long candidate, long defaultValue) {
        if (candidate <= 0) {
            return defaultValue;
        }
        return candidate;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
