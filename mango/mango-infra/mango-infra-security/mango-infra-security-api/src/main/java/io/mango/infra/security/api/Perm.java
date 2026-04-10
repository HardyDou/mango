package io.mango.infra.security.api;

import java.lang.annotation.*;

/**
 * Permission annotation for method-level access control.
 * <p>
 * Format: {model}:{module}:{action}
 * Example: system:user:add, system:user:edit
 *
 * @author Mango
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Perm {

    /**
     * Permission code
     */
    String value();

    /**
     * Permission description
     */
    String desc() default "";
}
