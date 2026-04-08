package io.mango.dal.core;

import io.mango.dal.api.IKvStore;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MemoryXivStore implementation using ConcurrentHashMap with background cleanup.
 * Implements AutoCloseable for use with try-with-resources.
 */
public class MemoryXivStore implements IKvStore, AutoCloseable {

    private final ConcurrentHashMap<String, KvEntry> map = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "kv-memory-cleaner");
        t.setDaemon(true);
        return t;
    });

    public MemoryXivStore() {
        cleaner.scheduleAtFixedRate(() ->
            map.entrySet().removeIf(e -> e.getValue().expired())
        , 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public boolean put(String key, String value, long expireSeconds) {
        validateKey(key);
        KvEntry prev = map.putIfAbsent(key, new KvEntry(value, Instant.now().plusSeconds(expireSeconds)));
        return prev == null;
    }

    @Override
    public String get(String key) {
        validateKey(key);
        KvEntry entry = map.get(key);
        if (entry == null || entry.expired()) {
            map.remove(key);
            return null;
        }
        return entry.value();
    }

    @Override
    public long increment(String key, long windowSeconds) {
        validateKey(key);
        synchronized (map) {
            KvEntry existing = map.get(key);
            Instant expiry = Instant.now().plusSeconds(windowSeconds);
            if (existing == null || existing.expired()) {
                map.put(key, new KvEntry("1", expiry));
                return 1;
            }
            long newCount = Long.parseLong(existing.value()) + 1;
            map.put(key, new KvEntry(String.valueOf(newCount), expiry));
            return newCount;
        }
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        map.remove(key);
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        KvEntry entry = map.get(key);
        return entry != null && !entry.expired();
    }

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

    private static final class KvEntry {
        private final String value;
        private final Instant expireTime;

        KvEntry(String value, Instant expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        String value() { return value; }
        boolean expired() { return expireTime.isBefore(Instant.now()); }
    }
}
