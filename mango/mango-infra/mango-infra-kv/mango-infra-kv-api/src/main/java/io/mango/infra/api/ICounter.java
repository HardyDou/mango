package io.mango.infra.kv.api;

/**
 * Atomic counter interface with rolling window support.
 * TTL is a first-class parameter - never hardcode TTL values.
 */
public interface ICounter {

    /**
     * Increment counter by delta within a time window.
     * Automatically sets expiration to windowSeconds.
     * @param key           counter key (must not be null or blank after trim)
     * @param delta         increment value (positive or negative)
     * @param windowSeconds rolling window in seconds, must be positive
     * @return new counter value
     */
    long increment(String key, long delta, long windowSeconds);

    /**
     * Get current counter value.
     * @param key counter key (must not be null or blank after trim)
     * @return current value or 0 if not found
     */
    long get(String key);
}