package io.mango.infra.sensitive.core;

import io.mango.infra.sensitive.api.ISensitiveMaskingService;
import io.mango.infra.sensitive.api.annotation.Sensitive;

/**
 * Default masking policy: annotated output is masked.
 */
public class DefaultSensitiveMaskingService implements ISensitiveMaskingService {

    @Override
    public boolean shouldMask(Sensitive sensitive) {
        return true;
    }
}
