package io.mango.dal.core;

import io.mango.dal.api.ICounter;

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
        synchronized (counters) {
            CounterEntry existing = counters.get(key);
            Instant now = Instant.now();
            if (existing == null || existing.expired(now)) {
                counters.put(key, new CounterEntry(String.valueOf(delta), now.plusSeconds(windowSeconds)));
                return delta;
            }
            long newValue = Long.parseLong(existing.value()) + delta;
            counters.put(key, new CounterEntry(String.valueOf(newValue), existing.expireTime()));
            return newValue;
        }
    }

    @Override
    public long get(String key) {
        validateKey(key);
        CounterEntry entry = counters.get(key);
        if (entry == null || entry.expired(Instant.now())) {
            return 0;
        }
        return Long.parseLong(entry.value());
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