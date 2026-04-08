package io.mango.dal.core;

import io.mango.dal.api.IIdempotent;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memory implementation of IIdempotent using ConcurrentHashMap.
 * WARNING: This is a local idempotency store, NOT distributed. Only suitable for single-instance deployments.
 */
public class MemoryIdempotent implements IIdempotent {

    private final ConcurrentHashMap<String, IdempotentEntry> entries = new ConcurrentHashMap<>();

    @Override
    public boolean isDuplicate(String key, long windowSeconds) {
        validateKey(key);
        validateWindow(windowSeconds);
        IdempotentEntry entry = entries.get(key);
        Instant now = Instant.now();
        if (entry == null || entry.expired(now)) {
            return false;
        }
        return true;
    }

    @Override
    public void mark(String key, long windowSeconds) {
        validateKey(key);
        validateWindow(windowSeconds);
        entries.put(key, new IdempotentEntry(Instant.now().plusSeconds(windowSeconds)));
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

    private static final class IdempotentEntry {
        private final Instant expireTime;

        IdempotentEntry(Instant expireTime) {
            this.expireTime = expireTime;
        }

        boolean expired(Instant now) {
            return expireTime.isBefore(now);
        }
    }
}