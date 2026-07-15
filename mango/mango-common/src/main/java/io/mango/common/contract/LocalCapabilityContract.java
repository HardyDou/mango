package io.mango.common.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an in-process JVM capability contract that is not an HTTP API protocol.
 *
 * <p>The marker is restricted by Mango architecture checks to contracts under
 * {@code io.mango.infra.*}. A marked contract must not be implemented by an HTTP Controller or
 * Feign adapter.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface LocalCapabilityContract {
}
