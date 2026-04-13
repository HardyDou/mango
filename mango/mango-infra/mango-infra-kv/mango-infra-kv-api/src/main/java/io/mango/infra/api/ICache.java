package io.mango.infra.kv.api;

/**
 * Generic cache interface with TTL support.
 * TTL is a first-class parameter - never hardcode TTL values.
 */
public interface ICache {

    /**
     * Set a key-value pair with TTL.
     * @param key   key (must not be null or blank after trim)
     * @param value value (must not be null)
     * @param ttlSeconds expiration in seconds, must be positive
     */
    void set(String key, String value, long ttlSeconds);

    /**
     * Get value by key.
     * @param key key (must not be null or blank after trim)
     * @return value or null if not found or expired
     */
    String get(String key);

    /**
     * Check if key exists (without considering expiration).
     * @param key key (must not be null or blank after trim)
     * @return true if key exists (may be expired)
     */
    boolean exists(String key);

    /**
     * Delete a key.
     * @param key key (must not be null or blank after trim)
     */
    void delete(String key);
}