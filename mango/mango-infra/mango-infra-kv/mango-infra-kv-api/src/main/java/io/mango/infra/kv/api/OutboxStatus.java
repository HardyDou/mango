package io.mango.infra.kv.api;

/**
 * Outbox message lifecycle status.
 */
public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED
}
