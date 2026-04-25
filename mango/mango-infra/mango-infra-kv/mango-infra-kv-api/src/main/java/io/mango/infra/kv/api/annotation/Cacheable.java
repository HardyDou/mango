package io.mango.infra.kv.api.annotation;

import java.lang.annotation.*;

/**
 * Cache method result with TTL support.
 * The cache key is generated from the method parameters.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {

    /**
     * Cache key expression.
     * Prefer SpEL template syntax for dynamic values.
     * Example: "user:#{#userId}:#{#req.headers['X-Tenant']}"
     */
    String key();

    /**
     * Cache TTL in seconds.
     * Must be positive.
     */
    long ttl() default 3600;

    /**
     * Whether to use the return value as the cache value.
     * If false, the annotation only marks the cache without storing.
     */
    boolean cacheValue() default true;
}
