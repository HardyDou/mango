package io.mango.infra.kv.api;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Outbox message persisted for reliable delivery.
 */
public class OutboxMessage {

    private String messageId = UUID.randomUUID().toString();
    private String eventType;
    private String businessType;
    private String businessKey;
    private String aggregateId;
    private Instant occurredAt = Instant.now();
    private OutboxStatus status = OutboxStatus.PENDING;
    private int attemptCount;
    private Instant nextAttemptAt;
    private Instant lockedAt;
    private String lockedBy;
    private String errorMessage;
    private Map<String, Object> payload = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();

    public OutboxMessage() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder()
                .messageId(messageId)
                .eventType(eventType)
                .businessType(businessType)
                .businessKey(businessKey)
                .aggregateId(aggregateId)
                .occurredAt(occurredAt)
                .status(status)
                .attemptCount(attemptCount)
                .nextAttemptAt(nextAttemptAt)
                .lockedAt(lockedAt)
                .lockedBy(lockedBy)
                .errorMessage(errorMessage)
                .payload(payload)
                .headers(headers);
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Instant lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload == null ? new HashMap<>() : new HashMap<>(payload);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? new HashMap<>() : new HashMap<>(headers);
    }

    public static final class Builder {
        private final OutboxMessage message = new OutboxMessage();

        private Builder() {
        }

        public Builder messageId(String messageId) {
            message.setMessageId(messageId);
            return this;
        }

        public Builder eventType(String eventType) {
            message.setEventType(eventType);
            return this;
        }

        public Builder businessType(String businessType) {
            message.setBusinessType(businessType);
            return this;
        }

        public Builder businessKey(String businessKey) {
            message.setBusinessKey(businessKey);
            return this;
        }

        public Builder aggregateId(String aggregateId) {
            message.setAggregateId(aggregateId);
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            message.setOccurredAt(occurredAt);
            return this;
        }

        public Builder status(OutboxStatus status) {
            message.setStatus(status);
            return this;
        }

        public Builder attemptCount(int attemptCount) {
            message.setAttemptCount(attemptCount);
            return this;
        }

        public Builder nextAttemptAt(Instant nextAttemptAt) {
            message.setNextAttemptAt(nextAttemptAt);
            return this;
        }

        public Builder lockedAt(Instant lockedAt) {
            message.setLockedAt(lockedAt);
            return this;
        }

        public Builder lockedBy(String lockedBy) {
            message.setLockedBy(lockedBy);
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            message.setErrorMessage(errorMessage);
            return this;
        }

        public Builder payload(Map<String, Object> payload) {
            message.setPayload(payload);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            message.setHeaders(headers);
            return this;
        }

        public OutboxMessage build() {
            return message;
        }
    }
}
