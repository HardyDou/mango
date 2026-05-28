package io.mango.infra.sensitive.api;

import io.mango.infra.sensitive.api.annotation.Sensitive;

/**
 * Decides whether an annotated output value should be masked for the current call.
 */
public interface ISensitiveMaskingService {

    /**
     * Returns true when the current output should be masked.
     *
     * @param sensitive field masking annotation
     * @return true to mask output, false to emit the original value
     */
    boolean shouldMask(Sensitive sensitive);
}
