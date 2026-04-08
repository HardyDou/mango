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
        LockEntry existing = locks.get(key);
        if (existing != null && !existing.expired()) {
            return false;
        }
        locks.put(key, new LockEntry(expireTime));
        return true;
    }

    @Override
    public void unlock(String key) {
        validateKey(key);
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