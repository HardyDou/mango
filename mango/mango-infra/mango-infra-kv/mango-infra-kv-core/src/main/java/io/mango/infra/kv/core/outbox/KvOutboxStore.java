package io.mango.infra.kv.core.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.infra.kv.api.IKvSortedSet;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxMessageQuery;
import io.mango.infra.kv.api.OutboxStatus;
import lombok.RequiredArgsConstructor;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
            sortedSet.add(allIndexKey(), message.getMessageId(), message.getOccurredAt().toEpochMilli(), pendingTtlSeconds);
        }
    }

    @Override
    public List<OutboxMessage> claim(String workerId, int batchSize, Instant now) {
        return claim(workerId, null, batchSize, now);
    }

    @Override
    public List<OutboxMessage> claim(String workerId, String eventType, int batchSize, Instant now) {
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
                if (eventType != null && !eventType.equals(message.getEventType())) {
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

    @Override
    public void fail(String messageId, String workerId, String errorMessage, Instant now) {
        validateWorker(workerId);
        OutboxMessage message = read(messageId);
        if (message == null) {
            return;
        }
        if (!workerId.equals(message.getLockedBy())) {
            return;
        }
        OutboxMessage failed = message.toBuilder()
                .status(OutboxStatus.FAILED)
                .lockedBy(workerId)
                .lockedAt(now)
                .errorMessage(errorMessage)
                .nextAttemptAt(null)
                .build();
        kvStore.set(messageKey(messageId), write(failed), pendingTtlSeconds);
        removeFromIndex(messageId);
        addToAllIndex(messageId, failed);
    }

    @Override
    public void requeue(String messageId, Instant nextAttemptAt, Instant now) {
        Require.notBlank(messageId, "messageId cannot be blank");
        OutboxMessage message = read(messageId);
        if (message == null) {
            return;
        }
        Instant readyAt = nextAttemptAt == null ? now : nextAttemptAt;
        OutboxMessage requeued = message.toBuilder()
                .status(OutboxStatus.PENDING)
                .lockedBy(null)
                .lockedAt(null)
                .errorMessage(null)
                .nextAttemptAt(readyAt)
                .build();
        kvStore.set(messageKey(messageId), write(requeued), pendingTtlSeconds);
        if (kvStore instanceof IKvSortedSet sortedSet) {
            sortedSet.add(pendingIndexKey(), messageId, readyAt.toEpochMilli(), pendingTtlSeconds);
            sortedSet.add(allIndexKey(), messageId, requeued.getOccurredAt().toEpochMilli(), pendingTtlSeconds);
        }
    }

    @Override
    public OutboxMessage findById(String messageId) {
        Require.notBlank(messageId, "messageId cannot be blank");
        return read(messageId);
    }

    @Override
    public List<OutboxMessage> query(OutboxMessageQuery query) {
        OutboxMessageQuery normalized = query == null ? new OutboxMessageQuery() : query;
        List<OutboxMessage> messages = loadAllMessages().stream()
                .filter(message -> matches(message, normalized))
                .sorted(Comparator.comparing(OutboxMessage::getOccurredAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(OutboxMessage::getMessageId))
                .toList();
        int fromIndex = (int) Math.min(messages.size(), (normalized.getPageNum() - 1L) * normalized.getPageSize());
        int toIndex = (int) Math.min(messages.size(), fromIndex + normalized.getPageSize());
        return messages.subList(fromIndex, toIndex);
    }

    @Override
    public long count(OutboxMessageQuery query) {
        OutboxMessageQuery normalized = query == null ? new OutboxMessageQuery() : query;
        return loadAllMessages().stream().filter(message -> matches(message, normalized)).count();
    }

    private void removeFromIndex(String messageId) {
        if (kvStore instanceof IKvSortedSet sortedSet) {
            sortedSet.remove(pendingIndexKey(), messageId);
        }
    }

    private void addToAllIndex(String messageId, OutboxMessage message) {
        if (kvStore instanceof IKvSortedSet sortedSet) {
            Instant occurredAt = message.getOccurredAt() == null ? Instant.EPOCH : message.getOccurredAt();
            sortedSet.add(allIndexKey(), messageId, occurredAt.toEpochMilli(), pendingTtlSeconds);
        }
    }

    private List<OutboxMessage> loadAllMessages() {
        if (!(kvStore instanceof IKvSortedSet sortedSet)) {
            return List.of();
        }
        Collection<String> messageIds = sortedSet.rangeByScore(allIndexKey(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0);
        List<OutboxMessage> messages = new ArrayList<>();
        for (String messageId : messageIds) {
            OutboxMessage message = read(messageId);
            if (message == null) {
                sortedSet.remove(allIndexKey(), messageId);
                continue;
            }
            messages.add(message);
        }
        return messages;
    }

    private boolean matches(OutboxMessage message, OutboxMessageQuery query) {
        if (query.isAbnormalOnly() && !isAbnormal(message)) {
            return false;
        }
        if (query.getStatus() != null && message.getStatus() != query.getStatus()) {
            return false;
        }
        if (!matchesText(message.getEventType(), query.getEventType(), false)) {
            return false;
        }
        if (!matchesText(message.getBusinessType(), query.getBusinessType(), false)) {
            return false;
        }
        if (!matchesText(message.getBusinessKey(), query.getBusinessKey(), false)) {
            return false;
        }
        return matchesKeyword(message, query.getKeyword());
    }

    private boolean isAbnormal(OutboxMessage message) {
        return message.getStatus() == OutboxStatus.FAILED
                || (message.getStatus() == OutboxStatus.PENDING && message.getAttemptCount() > 0)
                || message.getStatus() == OutboxStatus.PROCESSING;
    }

    private boolean matchesKeyword(OutboxMessage message, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return matchesText(message.getMessageId(), keyword, true)
                || matchesText(message.getEventType(), keyword, true)
                || matchesText(message.getBusinessType(), keyword, true)
                || matchesText(message.getBusinessKey(), keyword, true)
                || matchesText(message.getAggregateId(), keyword, true);
    }

    private boolean matchesText(String actual, String expected, boolean contains) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        String normalizedActual = actual.toLowerCase(Locale.ROOT);
        String normalizedExpected = expected.trim().toLowerCase(Locale.ROOT);
        return contains ? normalizedActual.contains(normalizedExpected) : Objects.equals(normalizedActual, normalizedExpected);
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

    private String allIndexKey() {
        return OutboxKeys.ALL;
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
