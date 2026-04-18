package io.mango.infra.kv.api.annotation;

import java.lang.annotation.*;

/**
 * Rate limit using token bucket algorithm.
 * Returns false when rate limit is exceeded.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Rate limiter key expression.
     * Prefer SpEL template syntax for dynamic values.
     * Example: "api:#{#userId}"
     */
    String key();

    /**
     * Number of permits to acquire.
     * Default is 1 (single request).
     */
    int permits() default 1;
}
