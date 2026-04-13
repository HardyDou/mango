package io.mango.infra.kv.api;

/**
 * Distributed lock interface.
 * TTL is a first-class parameter - never hardcode TTL values.
 */
public interface ILocker {

    /**
     * Try to acquire a lock.
     * @param key          lock key (must not be null or blank after trim)
     * @param ttlSeconds   lock TTL in seconds, must be positive
     * @return true if lock acquired, false if already held
     */
    boolean tryLock(String key, long ttlSeconds);

    /**
     * Release a lock.
     * @param key lock key (must not be null or blank after trim)
     */
    void unlock(String key);
}