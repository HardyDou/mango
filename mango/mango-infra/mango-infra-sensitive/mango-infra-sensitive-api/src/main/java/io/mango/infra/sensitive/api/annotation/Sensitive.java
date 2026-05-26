package io.mango.infra.sensitive.api.annotation;

import io.mango.infra.sensitive.api.enums.SensitiveType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String field or accessor for output masking.
 *
 * <p>The annotation is interpreted by the Mango sensitive Jackson module. It
 * does not mutate the annotated object's in-memory value.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Sensitive {

    /**
     * Masking strategy.
     *
     * @return strategy type
     */
    SensitiveType type() default SensitiveType.CUSTOM;

    /**
     * Number of leading characters kept as plain text for custom masking.
     *
     * @return visible prefix length
     */
    int prefixNoMaskLen() default 0;

    /**
     * Number of trailing characters kept as plain text for custom masking.
     *
     * @return visible suffix length
     */
    int suffixNoMaskLen() default 0;

    /**
     * Replacement token used by custom masking.
     *
     * @return mask token
     */
    String maskStr() default "*";

    /**
     * JSON keys whose values should be masked when {@link #type()} is
     * {@link SensitiveType#JSON}.
     *
     * @return JSON keys
     */
    String[] keys() default {};

    /**
     * Whether JSON key matching should use case-insensitive contains matching.
     *
     * @return true when fuzzy key matching is enabled
     */
    boolean fuzzy() default false;
}
