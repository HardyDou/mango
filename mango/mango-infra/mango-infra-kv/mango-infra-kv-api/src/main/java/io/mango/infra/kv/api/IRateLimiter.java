package io.mango.infra.kv.api;

/**
 * Fixed-window rate limiter interface.
 * TTL is a first-class parameter - never hardcode TTL values.
 */
public interface IRateLimiter {

    /**
     * Try to acquire permits from the rate limiter.
     * @param key      rate limiter key (must not be null or blank after trim)
     * @param permits  number of permits to acquire, must be positive
     * @return true if permits acquired, false if rate limit exceeded
     */
    boolean tryAcquire(String key, int permits);
}
