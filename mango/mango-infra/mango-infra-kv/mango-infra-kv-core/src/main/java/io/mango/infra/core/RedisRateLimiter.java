package io.mango.infra.kv.core;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.api.IRateLimiter;
import lombok.RequiredArgsConstructor;

/**
 * Redis implementation of IRateLimiter using IKvStore.
 * Fixed window rate limiter - uses increment to count requests.
 */
@RequiredArgsConstructor
public class RedisRateLimiter implements IRateLimiter {

    private final IKvStore kvStore;
    private final int defaultLimit = 100; // 默认限流阈值
    private final long defaultWindowSeconds = 60; // 默认窗口秒数

    @Override
    public boolean tryAcquire(String key, int permits) {
        return tryAcquire(key, permits, defaultLimit, defaultWindowSeconds);
    }

    /**
     * Try to acquire permits with custom limit and window.
     */
    public boolean tryAcquire(String key, int permits, int limit, long windowSeconds) {
        validateKey(key);
        if (permits <= 0) {
            throw new IllegalArgumentException("permits must be positive");
        }
        if (kvStore.exists(key)) {
            String current = kvStore.get(key);
            long count = current == null ? 0 : Long.parseLong(current);
            if (count + permits > limit) {
                return false;
            }
        }
        kvStore.increment(key, windowSeconds);
        return true;
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }
}
