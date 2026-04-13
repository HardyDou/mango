package io.mango.infra.kv.core;

import io.mango.infra.kv.api.IRateLimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Memory implementation of IRateLimiter using token bucket algorithm.
 * WARNING: This is a local rate limiter, NOT distributed. Only suitable for single-instance deployments.
 */
public class MemoryRateLimiter implements IRateLimiter {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int permits) {
        validateKey(key);
        if (permits <= 0) {
            throw new IllegalArgumentException("permits must be positive");
        }
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket());
        return bucket.tryAcquire(permits);
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }

    private static final class TokenBucket {
        private final AtomicInteger tokens = new AtomicInteger(100);  // Start with full bucket
        private volatile long lastRefillTime = System.currentTimeMillis();

        synchronized boolean tryAcquire(int permits) {
            refill();
            if (tokens.get() >= permits) {
                tokens.addAndGet(-permits);
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            int toAdd = (int) Math.min(100, elapsed / 1000);
            if (toAdd > 0) {
                tokens.addAndGet(toAdd);
                lastRefillTime = now;
            }
        }
    }
}