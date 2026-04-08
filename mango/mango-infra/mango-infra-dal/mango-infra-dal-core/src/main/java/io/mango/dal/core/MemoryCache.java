package io.mango.dal.core;

import io.mango.dal.api.ICache;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

/**
 * Memory implementation of ICache using ConcurrentHashMap with background cleanup.
 */
public class MemoryCache implements ICache, AutoCloseable {

    private final ConcurrentHashMap<String, CacheEntry> map = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cache-memory-cleaner");
        t.setDaemon(true);
        return t;
    });

    public MemoryCache() {
        this(1);
    }

    public MemoryCache(int cleanupIntervalMinutes) {
        cleaner.scheduleAtFixedRate(() ->
            map.entrySet().removeIf(e -> e.getValue().expired())
        , cleanupIntervalMinutes, cleanupIntervalMinutes, TimeUnit.MINUTES);
    }

    @Override
    public void set(String key, String value, long ttlSeconds) {
        validateKey(key);
        validateValue(value);
        validateTtl(ttlSeconds);
        map.put(key, new CacheEntry(value, Instant.now().plusSeconds(ttlSeconds)));
    }

    @Override
    public String get(String key) {
        validateKey(key);
        CacheEntry entry = map.get(key);
        if (entry == null || entry.expired()) {
            map.remove(key);
            return null;
        }
        return entry.value();
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        CacheEntry entry = map.get(key);
        return entry != null && !entry.expired();
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        map.remove(key);
    }

    @PreDestroy
    @Override
    public void close() {
        cleaner.shutdown();
        try {
            if (!cleaner.awaitTermination(5, TimeUnit.SECONDS)) {
                cleaner.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleaner.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }

    private void validateValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
    }

    private void validateTtl(long ttlSeconds) {
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("ttlSeconds must be positive");
        }
    }

    private static final class CacheEntry {
        private final String value;
        private final Instant expireTime;

        CacheEntry(String value, Instant expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        String value() { return value; }
        boolean expired() { return expireTime.isBefore(Instant.now()); }
    }
}