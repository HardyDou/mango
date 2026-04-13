package io.mango.infra.kv.core;

import io.mango.infra.kv.api.IIdempotent;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;

/**
 * Redis implementation of IIdempotent using IKvStore.
 */
@RequiredArgsConstructor
public class RedisIdempotent implements IIdempotent {

    private final IKvStore kvStore;

    @Override
    public boolean isDuplicate(String key, long windowSeconds) {
        validateKey(key);
        validateWindow(windowSeconds);
        return kvStore.exists(key);
    }

    @Override
    public void mark(String key, long windowSeconds) {
        validateKey(key);
        validateWindow(windowSeconds);
        kvStore.put(key, "1", windowSeconds);
    }

    @Override
    public boolean checkAndMark(String key, long windowSeconds) {
        validateKey(key);
        validateWindow(windowSeconds);
        // put returns true if new (not duplicate), false if exists (duplicate)
        return !kvStore.put(key, "1", windowSeconds);
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
