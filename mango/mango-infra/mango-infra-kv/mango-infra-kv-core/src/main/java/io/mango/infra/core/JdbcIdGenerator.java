package io.mango.infra.kv.core;

import io.mango.infra.kv.api.IIdGenerator;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;

/**
 * JDBC implementation of IIdGenerator using IKvStore.
 * Uses IKvStore.increment for atomic ID generation.
 */
@RequiredArgsConstructor
public class JdbcIdGenerator implements IIdGenerator {

    private final IKvStore kvStore;
    private static final String KEY_PREFIX = "idgen:";
    private static final long DEFAULT_WINDOW_SECONDS = 86400; // 1 day

    @Override
    public long nextId() {
        return kvStore.increment(KEY_PREFIX + "global", DEFAULT_WINDOW_SECONDS);
    }

    /**
     * Generate next ID for a specific key.
     */
    public long nextId(String key) {
        return kvStore.increment(KEY_PREFIX + key, DEFAULT_WINDOW_SECONDS);
    }
}
