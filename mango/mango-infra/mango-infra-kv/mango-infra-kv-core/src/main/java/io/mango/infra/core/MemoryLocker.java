package io.mango.infra.kv.core;

import io.mango.infra.kv.api.ILocker;

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
        long holderThreadId = Thread.currentThread().getId();
        LockEntry newEntry = new LockEntry(expireTime, holderThreadId);
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
        // Verify holder before removing — prevents releasing another thread's lock
        // (can happen if lock expired and another thread acquired the same key)
        long callerId = Thread.currentThread().getId();
        locks.compute(key, (k, existing) -> {
            if (existing != null && existing.holderThreadId() == callerId) {
                return null; // remove only if we own it
            }
            return existing; // don't remove if not our lock
        });
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
        private final long holderThreadId;

        LockEntry(long expireTime, long holderThreadId) {
            this.expireTime = expireTime;
            this.holderThreadId = holderThreadId;
        }

        boolean expired() {
            return System.currentTimeMillis() > expireTime;
        }

        long holderThreadId() {
            return holderThreadId;
        }
    }
}
