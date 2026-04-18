package io.mango.infra.kv.api.annotation;

import java.lang.annotation.*;

/**
 * Mark method as idempotent to prevent duplicate processing.
 *
 * <p><b>Semantics: Mark-before.</b> The idempotency key is checked and marked
 * <i>before</i> method execution. If the method throws after the key is marked,
 * subsequent retry attempts with the same key will be rejected as duplicates
 * (until the window expires). Callers should handle the duplicate-operation
 * exception raised by the runtime adapter appropriately: wait, use a different
 * key, or escalate.
 *
 * <p>If you need mark-after semantics (allow retry after transient failure),
 * implement retry-with-exception handling at the caller level.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Idempotency key expression.
     * Prefer SpEL template syntax for dynamic values.
     * Example: "order:#{#orderNo}"
     */
    String key();

    /**
     * Detection window in seconds.
     * Must be positive.
     */
    long window() default 60;
}
