package io.mango.infra.kv.core.outbox;

/**
 * Outbox key prefixes.
 */
public final class OutboxKeys {

    public static final String MESSAGE = "message";
    public static final String PENDING = "pending";
    public static final String ALL = "all";
    public static final String TOPIC_SEPARATOR = ":";

    private OutboxKeys() {
    }
}
