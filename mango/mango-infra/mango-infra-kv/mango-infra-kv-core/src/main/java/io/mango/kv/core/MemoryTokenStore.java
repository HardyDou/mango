package io.mango.kv.core;

import io.mango.kv.api.ITokenStore;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

/**
 * Memory implementation of ITokenStore using ConcurrentHashMap with background cleanup.
 */
public class MemoryTokenStore implements ITokenStore, AutoCloseable {

    private final ConcurrentHashMap<String, TokenEntry> tokens = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "token-memory-cleaner");
        t.setDaemon(true);
        return t;
    });

    public MemoryTokenStore() {
        this(1);
    }

    public MemoryTokenStore(int cleanupIntervalMinutes) {
        cleaner.scheduleAtFixedRate(() ->
            tokens.entrySet().removeIf(e -> e.getValue().expired())
        , cleanupIntervalMinutes, cleanupIntervalMinutes, TimeUnit.MINUTES);
    }

    @Override
    public void store(String token, String value, long ttlSeconds) {
        validateKey(token);
        validateValue(value);
        validateTtl(ttlSeconds);
        tokens.put(token, new TokenEntry(value, Instant.now().plusSeconds(ttlSeconds)));
    }

    @Override
    public String get(String token) {
        validateKey(token);
        TokenEntry entry = tokens.get(token);
        if (entry == null || entry.expired()) {
            tokens.remove(token);
            return null;
        }
        return entry.value();
    }

    @Override
    public void remove(String token) {
        validateKey(token);
        tokens.remove(token);
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
            throw new IllegalArgumentException("token cannot be null or blank");
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

    private static final class TokenEntry {
        private final String value;
        private final Instant expireTime;

        TokenEntry(String value, Instant expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        String value() { return value; }
        boolean expired() { return expireTime.isBefore(Instant.now()); }
    }
}