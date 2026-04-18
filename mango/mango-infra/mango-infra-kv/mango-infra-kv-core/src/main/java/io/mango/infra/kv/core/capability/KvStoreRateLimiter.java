package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.api.IRateLimiter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KvStoreRateLimiter implements IRateLimiter {

    private static final int DEFAULT_LIMIT = 100;
    private static final long DEFAULT_WINDOW_SECONDS = 60;

    private final IKvStore kvStore;

    @Override
    public boolean tryAcquire(String key, int permits) {
        return tryAcquire(key, permits, DEFAULT_LIMIT, DEFAULT_WINDOW_SECONDS);
    }

    public boolean tryAcquire(String key, int permits, int limit, long windowSeconds) {
        validateKey(key);
        if (permits <= 0) {
            throw new IllegalArgumentException("permits must be positive");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive");
        }
        return kvStore.incrementBy(key, permits, windowSeconds) <= limit;
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }
}
