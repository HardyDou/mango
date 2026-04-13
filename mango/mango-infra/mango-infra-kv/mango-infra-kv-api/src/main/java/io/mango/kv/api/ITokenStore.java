package io.mango.kv.api;

/**
 * Token storage interface for authentication tokens, session tokens, etc.
 * TTL is a first-class parameter - never hardcode TTL values.
 */
public interface ITokenStore {

    /**
     * Store a token with associated value and TTL.
     * @param token         token key (must not be null or blank after trim)
     * @param value         token value (must not be null)
     * @param ttlSeconds    expiration in seconds, must be positive
     */
    void store(String token, String value, long ttlSeconds);

    /**
     * Retrieve token value.
     * @param token token key (must not be null or blank after trim)
     * @return token value or null if not found or expired
     */
    String get(String token);

    /**
     * Remove a token.
     * @param token token key (must not be null or blank after trim)
     */
    void remove(String token);
}