package io.mango.infra.kv.api;

/**
 * Unified KV storage abstraction for replay protection, idempotency, and rate limiting.
 */
public interface IKvStore {

    /**
     * Write a key only if it does not already exist, with specified expiration time (seconds).
     * Existing expired entries must be treated as absent.
     * @param key key (must not be null or blank after trim)
     * @param value value
     * @param expireSeconds expiration in seconds (must be positive)
     * @return true=new key added, false=key already exists
     */
    default boolean setIfAbsent(String key, String value, long expireSeconds) {
        return put(key, value, expireSeconds);
    }

    /**
     * Write or replace a key with specified expiration time (seconds).
     * This is the correct operation for cache and token values.
     * @param key key (must not be null or blank after trim)
     * @param value value
     * @param expireSeconds expiration in seconds (must be positive)
     */
    default void set(String key, String value, long expireSeconds) {
        delete(key);
        setIfAbsent(key, value, expireSeconds);
    }

    /**
     * Legacy alias for setIfAbsent.
     * @param key key (must not be null or blank after trim)
     * @param value value
     * @param expireSeconds expiration in seconds (must be positive)
     * @return true=new key added, false=key already exists
     */
    default boolean put(String key, String value, long expireSeconds) {
        return setIfAbsent(key, value, expireSeconds);
    }

    /**
     * Handle TTL <= 0: delete key immediately and return false.
     * All implementations share this contract. Redis can also throw if Redis
     * semantics forbid negative TTL (but the interface contract says delete+false).
     * @param key key (assumed pre-validated)
     * @return false (key was not stored due to non-positive TTL)
     */
    default boolean putNonPositiveTtl(String key) {
        delete(key);
        return false;
    }

    /**
     * Read a key, returns null if expired or not found.
     * @param key key (must not be null or blank after trim)
     * @return value or null
     */
    String get(String key);

    /**
     * Increment counter, automatically sets expiration to windowSeconds.
     * @param key key (must not be null or blank after trim)
     * @param windowSeconds rolling window in seconds
     * @return incremented value
     */
    default long increment(String key, long windowSeconds) {
        return incrementBy(key, 1, windowSeconds);
    }

    /**
     * Increment counter by delta, automatically sets expiration to windowSeconds.
     * Implementations must keep this operation atomic for a single key.
     * @param key key (must not be null or blank after trim)
     * @param delta increment value, may be positive or negative
     * @param windowSeconds rolling window in seconds
     * @return incremented value
     */
    default long incrementBy(String key, long delta, long windowSeconds) {
        if (delta < 0) {
            throw new UnsupportedOperationException("negative delta is not supported by this IKvStore implementation");
        }
        long current = 0;
        for (long i = 0; i < delta; i++) {
            current = increment(key, windowSeconds);
        }
        return current;
    }

    /**
     * Delete a key.
     * @param key key (must not be null or blank after trim)
     */
    void delete(String key);

    /**
     * Check if a key exists (without considering expiration).
     * Used for nonce blacklist check in internal call verification.
     * @param key key (must not be null or blank after trim)
     * @return true if key exists (may be expired)
     */
    boolean exists(String key);
}
