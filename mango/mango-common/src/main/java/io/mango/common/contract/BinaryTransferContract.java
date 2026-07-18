package io.mango.common.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a JVM transfer model that carries binary content and is not a JSON protocol model.
 *
 * <p>Use this marker only for local file-transfer boundaries containing values such as
 * {@link java.io.InputStream}. HTTP adapters must translate the marked model to multipart or
 * streaming HTTP types at the transport boundary.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface BinaryTransferContract {
}
