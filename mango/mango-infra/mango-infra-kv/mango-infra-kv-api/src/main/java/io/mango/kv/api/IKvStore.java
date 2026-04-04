package io.mango.kv.api;

/**
 * Unified KV storage abstraction for replay protection, idempotency, and rate limiting.
 */
public interface IKvStore {

    /**
     * Write a key with specified expiration time (seconds).
     * @param key key (must not be null or blank after trim)
     * @param value value
     * @param expireSeconds expiration in seconds
     * @return true=new key added, false=key already exists (for replay protection)
     */
    boolean put(String key, String value, long expireSeconds);

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
    long increment(String key, long windowSeconds);

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
