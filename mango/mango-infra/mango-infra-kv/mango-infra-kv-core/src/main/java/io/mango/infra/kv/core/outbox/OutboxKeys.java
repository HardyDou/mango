package io.mango.infra.kv.core.outbox;

/**
 * Outbox key prefixes.
 */
public final class OutboxKeys {

    public static final String MESSAGE = "message";
    public static final String PENDING = "pending";

    private OutboxKeys() {
    }
}
