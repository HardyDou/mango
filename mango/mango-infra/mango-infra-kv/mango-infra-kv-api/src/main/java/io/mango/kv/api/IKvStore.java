package io.mango.kv.api;

/**
 * Unified KV storage abstraction for replay protection, idempotency, and rate limiting.
 */
public interface IKvStore {

    /**
     * Write a key with specified expiration time (seconds).
     * @param key key (must not be null or blank after trim)
     * @param value value
     * @param expireSeconds expiration in seconds (must be positive)
     * @return true=new key added, false=key already exists (for replay protection)
     * @throws IllegalArgumentException if key is null or blank
     */
    boolean put(String key, String value, long expireSeconds);

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
