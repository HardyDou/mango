package io.mango.infra.kv.core.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.infra.kv.api.IKvSortedSet;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxStatus;
import lombok.RequiredArgsConstructor;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Outbox store backed by IKvStore.
 */
@RequiredArgsConstructor
public class KvOutboxStore implements IOutboxStore {

    public static final long DEFAULT_PENDING_TTL_SECONDS = 7L * 24 * 60 * 60;
    public static final long DEFAULT_LOCK_TTL_SECONDS = 60L;

    private final IKvStore kvStore;
    private final ObjectMapper objectMapper;
    private final long pendingTtlSeconds;
    private final long lockTtlSeconds;

    public KvOutboxStore(IKvStore kvStore, ObjectMapper objectMapper) {
        this(kvStore, objectMapper, DEFAULT_PENDING_TTL_SECONDS, DEFAULT_LOCK_TTL_SECONDS);
    }

    @Override
    public void enqueue(OutboxMessage message) {
        validateMessage(message);
        kvStore.set(messageKey(message.getMessageId()), write(message), pendingTtlSeconds);
        if (kvStore instanceof IKvSortedSet sortedSet) {
            sortedSet.add(pendingIndexKey(), message.getMessageId(), message.getOccurredAt().toEpochMilli(), pendingTtlSeconds);
        }
    }

    @Override
    public List<OutboxMessage> claim(String workerId, int batchSize, Instant now) {
        validateWorker(workerId);
        if (batchSize <= 0) {
            return List.of();
        }
        List<OutboxMessage> claimed = new ArrayList<>();
        if (kvStore instanceof IKvSortedSet sortedSet) {
            for (String messageId : sortedSet.rangeByScore(pendingIndexKey(), Double.NEGATIVE_INFINITY, now.toEpochMilli(), batchSize)) {
                OutboxMessage message = read(messageId);
                if (message == null) {
                    continue;
                }
                if (message.getStatus() == OutboxStatus.SUCCESS) {
                    continue;
                }
                if (message.getNextAttemptAt() != null && message.getNextAttemptAt().isAfter(now)) {
                    continue;
                }
                OutboxMessage locked = message.toBuilder()
                        .status(OutboxStatus.PROCESSING)
                        .lockedBy(workerId)
                        .lockedAt(now)
                        .attemptCount(message.getAttemptCount() + 1)
                        .nextAttemptAt(now.plusSeconds(lockTtlSeconds))
                        .build();
                kvStore.set(messageKey(messageId), write(locked), pendingTtlSeconds);
                sortedSet.add(pendingIndexKey(), messageId, locked.getNextAttemptAt().toEpochMilli(), pendingTtlSeconds);
                claimed.add(locked);
            }
        }
        claimed.sort(Comparator.comparing(OutboxMessage::getOccurredAt));
        return claimed;
    }

    @Override
    public void ack(String messageId, String workerId, Instant now) {
        validateWorker(workerId);
        OutboxMessage message = read(messageId);
        if (message == null) {
            return;
        }
        if (!workerId.equals(message.getLockedBy())) {
            return;
        }
        OutboxMessage done = message.toBuilder()
                .status(OutboxStatus.SUCCESS)
                .lockedBy(workerId)
                .lockedAt(now)
                .nextAttemptAt(null)
                .errorMessage(null)
                .build();
        kvStore.set(messageKey(messageId), write(done), pendingTtlSeconds);
        removeFromIndex(messageId);
    }

    @Override
    public void nack(String messageId, String workerId, String errorMessage, Instant nextAttemptAt, Instant now) {
        validateWorker(workerId);
        OutboxMessage message = read(messageId);
        if (message == null) {
            return;
        }
        if (!workerId.equals(message.getLockedBy())) {
            return;
        }
        OutboxMessage failed = message.toBuilder()
                .status(OutboxStatus.PENDING)
                .lockedBy(workerId)
                .lockedAt(now)
                .errorMessage(errorMessage)
                .nextAttemptAt(nextAttemptAt == null ? now.plusSeconds(lockTtlSeconds) : nextAttemptAt)
                .build();
        kvStore.set(messageKey(messageId), write(failed), pendingTtlSeconds);
        if (kvStore instanceof IKvSortedSet sortedSet) {
            sortedSet.add(pendingIndexKey(), messageId, failed.getNextAttemptAt().toEpochMilli(), pendingTtlSeconds);
        }
    }

    private void removeFromIndex(String messageId) {
        if (kvStore instanceof IKvSortedSet sortedSet) {
            sortedSet.remove(pendingIndexKey(), messageId);
        }
    }

    private OutboxMessage read(String messageId) {
        String payload = kvStore.get(messageKey(messageId));
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, OutboxMessage.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to read outbox message: " + messageId, ex);
        }
    }

    private String write(OutboxMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String messageKey(String messageId) {
        return OutboxKeys.MESSAGE + ":" + messageId;
    }

    private String pendingIndexKey() {
        return OutboxKeys.PENDING;
    }

    private void validateMessage(OutboxMessage message) {
        Require.notNull(message, "message cannot be null");
        Require.notBlank(message.getMessageId(), "messageId cannot be blank");
        Require.notBlank(message.getEventType(), "eventType cannot be blank");
    }

    private void validateWorker(String workerId) {
        Require.notBlank(workerId, "workerId cannot be blank");
    }
}
