package io.mango.common.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an in-process JVM capability boundary that is not an HTTP API protocol.
 *
 * <p>The marker is restricted by Mango architecture checks to types under
 * {@code io.mango.infra.*}. It normally marks contracts and local input/output types. A concrete
 * capability entry may use it only when its public legacy type name must remain compatible. A
 * marked boundary must not be implemented or exposed by an HTTP Controller or Feign adapter.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface LocalCapabilityContract {
}
