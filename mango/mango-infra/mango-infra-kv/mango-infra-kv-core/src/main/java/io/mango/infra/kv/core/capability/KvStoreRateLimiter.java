package io.mango.infra.kv.core.capability;

import io.mango.common.result.Require;
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
        Require.positive(permits, "permits must be positive");
        Require.positive(limit, "limit must be positive");
        Require.positive(windowSeconds, "windowSeconds must be positive");
        return kvStore.incrementBy(key, permits, windowSeconds) <= limit;
    }

    private void validateKey(String key) {
        Require.notBlank(key, "key cannot be null or blank");
    }
}
