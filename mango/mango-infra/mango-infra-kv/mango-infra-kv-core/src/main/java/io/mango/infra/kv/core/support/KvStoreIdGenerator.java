package io.mango.infra.kv.core.support;

import io.mango.infra.kv.api.IIdGenerator;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;

/**
 * Distributed ID generator backed by IKvStore.
 */
@RequiredArgsConstructor
public class KvStoreIdGenerator implements IIdGenerator {

    private static final long DEFAULT_WINDOW_SECONDS = 86400;

    private final IKvStore kvStore;

    @Override
    public long nextId() {
        return kvStore.incrementBy("global", 1, DEFAULT_WINDOW_SECONDS);
    }
}
