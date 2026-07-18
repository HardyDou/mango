package io.mango.common.contract;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an HTTP adapter whose established wire protocol cannot use the canonical JSON envelope.
 *
 * <p>Typical examples are SSE, long polling, protocol negotiation and internal message relays.
 * The marker only relaxes controller/API shape and configurable endpoint checks that would change
 * the wire protocol; transport binding, validation, OpenAPI and URI-template checks still apply.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NativeHttpAdapter {
}
