package io.mango.dal.core;

import io.mango.dal.api.IKvStore;
import jakarta.annotation.PreDestroy;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MemoryKvStore implementation using segmented ConcurrentHashMap with background cleanup.
 * Uses N buckets to reduce lock contention — operations on different keys rarely collide.
 * Implements AutoCloseable for use with try-with-resources.
 * Spring automatically calls close() via @PreDestroy when the bean is destroyed.
 */
public class MemoryKvStore implements IKvStore, AutoCloseable {

    private static final int BUCKET_COUNT = 32;

    private final ConcurrentHashMap<String, KvEntry>[] buckets;
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "kv-memory-cleaner");
        t.setDaemon(true);
        return t;
    });

    /**
     * 使用默认清理间隔（1分钟）和 32 个分桶
     */
    public MemoryKvStore() {
        this(1, BUCKET_COUNT);
    }

    /**
     * @param cleanupIntervalMinutes 过期 key 清理任务间隔（分钟）
     */
    public MemoryKvStore(int cleanupIntervalMinutes) {
        this(cleanupIntervalMinutes, BUCKET_COUNT);
    }

    /**
     * @param cleanupIntervalMinutes 过期 key 清理任务间隔（分钟）
     * @param bucketCount             分桶数量（必须是正整数）
     */
    @SuppressWarnings("unchecked")
    public MemoryKvStore(int cleanupIntervalMinutes, int bucketCount) {
        if (cleanupIntervalMinutes <= 0) {
            throw new IllegalArgumentException("cleanupIntervalMinutes must be positive");
        }
        if (bucketCount <= 0) {
            throw new IllegalArgumentException("bucketCount must be positive");
        }
        buckets = new ConcurrentHashMap[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            buckets[i] = new ConcurrentHashMap<>();
        }
        cleaner.scheduleAtFixedRate(this::cleanupExpired, cleanupIntervalMinutes, cleanupIntervalMinutes, TimeUnit.MINUTES);
    }

    private int bucketIndex(String key) {
        // Use & 0x7FFFFFFF instead of Math.abs to avoid Integer.MIN_VALUE overflow
        return (key.hashCode() & 0x7FFFFFFF) % buckets.length;
    }

    private ConcurrentHashMap<String, KvEntry> bucket(String key) {
        return buckets[bucketIndex(key)];
    }

    private void cleanupExpired() {
        for (ConcurrentHashMap<String, KvEntry> b : buckets) {
            b.entrySet().removeIf(e -> e.getValue().expired());
        }
    }

    @Override
    public boolean put(String key, String value, long expireSeconds) {
        validateKey(key);
        if (expireSeconds <= 0) {
            // TTL=0 或负数：立即删除，不存储
            bucket(key).remove(key);
            return false;
        }
        KvEntry prev = bucket(key).putIfAbsent(key, new KvEntry(value, Instant.now().plusSeconds(expireSeconds)));
        return prev == null;
    }

    @Override
    public String get(String key) {
        validateKey(key);
        KvEntry entry = bucket(key).get(key);
        if (entry == null || entry.expired()) {
            bucket(key).remove(key);
            return null;
        }
        return entry.value();
    }

    @Override
    public long increment(String key, long windowSeconds) {
        validateKey(key);
        ConcurrentHashMap<String, KvEntry> b = bucket(key);
        Instant expiry = Instant.now().plusSeconds(windowSeconds);
        // Per-bucket lock replaces global lock — less contention under high concurrency
        synchronized (b) {
            KvEntry existing = b.get(key);
            if (existing == null || existing.expired()) {
                b.put(key, new KvEntry("1", expiry));
                return 1;
            }
            long newCount;
            try {
                newCount = Long.parseLong(existing.value()) + 1;
            } catch (NumberFormatException e) {
                // Corrupted value — treat as expired, re-insert as new counter
                b.put(key, new KvEntry("1", expiry));
                return 1;
            }
            b.put(key, new KvEntry(String.valueOf(newCount), expiry));
            return newCount;
        }
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        bucket(key).remove(key);
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        KvEntry entry = bucket(key).get(key);
        return entry != null && !entry.expired();
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
