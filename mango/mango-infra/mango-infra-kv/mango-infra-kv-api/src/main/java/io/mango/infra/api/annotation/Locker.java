package io.mango.infra.kv.api.annotation;

import java.lang.annotation.*;

/**
 * Distributed lock around method execution.
 * Ensures only one instance executes the method at a time.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Locker {

    /**
     * Lock key.
     * SpEL expression supported using # prefix.
     * Example: "order:#orderId"
     */
    String key();

    /**
     * Lock TTL in seconds.
     * Must be positive.
     */
    long ttl() default 30;
}