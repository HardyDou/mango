package io.mango.dal.core;

import io.mango.dal.api.ILocker;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Memory implementation of ILocker using ConcurrentHashMap.
 * WARNING: This is a local lock, NOT distributed. Only suitable for single-instance deployments.
 */
public class MemoryLocker implements ILocker {

    private final ConcurrentHashMap<String, LockEntry> locks = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key, long ttlSeconds) {
        validateKey(key);
        validateTtl(ttlSeconds);
        long expireTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ttlSeconds);
        LockEntry newEntry = new LockEntry(expireTime);
        // Use compute() to atomically: insert if absent, or replace if existing entry expired
        // This avoids the putIfAbsent bug where expired entries were never replaced
        LockEntry result = locks.compute(key, (k, existing) -> {
            if (existing != null && !existing.expired()) {
                return existing; // keep valid lock, don't overwrite
            }
            return newEntry; // insert or replace expired entry
        });
        return result == newEntry; // true only if our entry was inserted
    }

    @Override
    public void unlock(String key) {
        if (key == null || key.trim().isEmpty()) {
            return; // no-op for invalid key — ILocker contract: unlock never throws
        }
        locks.remove(key);
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }

    private void validateTtl(long ttlSeconds) {
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("ttlSeconds must be positive");
        }
    }

    private static final class LockEntry {
        private final long expireTime;

        LockEntry(long expireTime) {
            this.expireTime = expireTime;
        }

        boolean expired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}