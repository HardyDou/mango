package io.mango.infra.kv.core;

import io.mango.infra.kv.api.IIdempotent;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Memory implementation of IIdempotent using ConcurrentHashMap.
 * WARNING: This is a local idempotency store, NOT distributed. Only suitable for single-instance deployments.
 */
public class MemoryIdempotent implements IIdempotent, AutoCloseable {

    private static final int DEFAULT_CLEANUP_INTERVAL_MINUTES = 1;

    private final ConcurrentHashMap<String, IdempotentEntry> entries = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner;

    /**
     * Uses default cleanup interval of 1 minute.
     */
    public MemoryIdempotent() {
        this(DEFAULT_CLEANUP_INTERVAL_MINUTES);
    }

    /**
     * @param cleanupIntervalMinutes expired entry cleanup interval in minutes
     */
    public MemoryIdempotent(int cleanupIntervalMinutes) {
        if (cleanupIntervalMinutes <= 0) {
            throw new IllegalArgumentException("cleanupIntervalMinutes must be positive");
        }
        cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "idempotent-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(
            this::cleanupExpired,
            cleanupIntervalMinutes,
            cleanupIntervalMinutes,
            TimeUnit.MINUTES
        );
    }

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

    @Override
    public boolean checkAndMark(String key, long windowSeconds) {
        validateKey(key);
        validateWindow(windowSeconds);
        Instant now = Instant.now();
        Instant expireTime = now.plusSeconds(windowSeconds);
        // isDuplicate[0] is set inside compute to avoid capturing non-final variables
        boolean[] isDuplicate = {false};
        entries.compute(key, (k, existing) -> {
            if (existing != null && !existing.expired(now)) {
                isDuplicate[0] = true;
                return existing; // keep existing, don't overwrite
            }
            isDuplicate[0] = false;
            return new IdempotentEntry(expireTime);
        });
        return isDuplicate[0];
    }

    private void cleanupExpired() {
        entries.entrySet().removeIf(e -> e.getValue().expired(Instant.now()));
    }

    @PreDestroy
    @Override
    public void close() {
        cleaner.shutdown();
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
