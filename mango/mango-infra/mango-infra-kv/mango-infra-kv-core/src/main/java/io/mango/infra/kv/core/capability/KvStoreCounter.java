package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.api.ICounter;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KvStoreCounter implements ICounter {

    private final IKvStore kvStore;

    @Override
    public long increment(String key, long delta, long windowSeconds) {
        validateKey(key);
        validateWindow(windowSeconds);
        return kvStore.incrementBy(key, delta, windowSeconds);
    }

    @Override
    public long get(String key) {
        validateKey(key);
        String value = kvStore.get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }

    private void validateWindow(long windowSeconds) {
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive");
        }
    }
}
