package io.mango.common.contract;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an HTTP adapter whose transport contract is inherently binary.
 *
 * <p>The marker is limited to upload, download and streaming object endpoints that cannot use the
 * canonical JSON {@code R<T>} envelope without changing their wire semantics. It does not relax
 * placement, root-path, validation, OpenAPI or service-port rules.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BinaryHttpAdapter {
}
