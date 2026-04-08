package io.mango.dal.api.annotation;

import java.lang.annotation.*;

/**
 * Mark method as idempotent to prevent duplicate processing.
 * Throws exception when duplicate is detected.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Idempotency key.
     * SpEL expression supported using # prefix.
     * Example: "order:#orderNo"
     */
    String key();

    /**
     * Detection window in seconds.
     * Must be positive.
     */
    long window() default 60;
}