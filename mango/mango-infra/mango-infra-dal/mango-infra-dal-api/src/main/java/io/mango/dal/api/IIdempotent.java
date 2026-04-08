package io.mango.dal.api;

/**
 * Idempotency / duplicate detection interface.
 * TTL is a first-class parameter - never hardcode TTL values.
 */
public interface IIdempotent {

    /**
     * Check if a key represents a duplicate operation.
     * @param key           idempotency key (must not be null or blank after trim)
     * @param windowSeconds detection window in seconds, must be positive
     * @return true if duplicate (key exists and not expired), false otherwise
     */
    boolean isDuplicate(String key, long windowSeconds);

    /**
     * Mark a key as processed.
     * @param key           idempotency key (must not be null or blank after trim)
     * @param windowSeconds validity window in seconds, must be positive
     */
    void mark(String key, long windowSeconds);
}