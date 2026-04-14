package io.mango.infra.kv.core;

import io.mango.infra.kv.api.ICounter;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memory implementation of ICounter using ConcurrentHashMap with atomic operations.
 */
public class MemoryCounter implements ICounter {

    private final ConcurrentHashMap<String, CounterEntry> counters = new ConcurrentHashMap<>();

    @Override
    public long increment(String key, long delta, long windowSeconds) {
        validateKey(key);
        validateWindow(windowSeconds);
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(windowSeconds);
        // Use compute() for atomic check-and-update — no global lock needed
        CounterEntry result = counters.compute(key, (k, existing) -> {
            if (existing == null || existing.expired(now)) {
                return new CounterEntry(String.valueOf(delta), expiry);
            }
            long newValue;
            try {
                newValue = Long.parseLong(existing.value()) + delta;
            } catch (NumberFormatException e) {
                // Corrupted value — treat as expired, re-initialize
                return new CounterEntry(String.valueOf(delta), expiry);
            }
            return new CounterEntry(String.valueOf(newValue), existing.expireTime());
        });
        return Long.parseLong(result.value());
    }

    @Override
    public long get(String key) {
        validateKey(key);
        CounterEntry entry = counters.get(key);
        if (entry == null || entry.expired(Instant.now())) {
            return 0;
        }
        try {
            return Long.parseLong(entry.value());
        } catch (NumberFormatException e) {
            return 0;
        }
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

    private static final class CounterEntry {
        private final String value;
        private final Instant expireTime;

        CounterEntry(String value, Instant expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        String value() { return value; }
        Instant expireTime() { return expireTime; }
        boolean expired(Instant now) { return expireTime.isBefore(now); }
    }
}
