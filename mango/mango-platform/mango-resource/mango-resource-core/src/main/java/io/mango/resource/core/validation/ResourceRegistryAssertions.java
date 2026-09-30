package io.mango.resource.core.validation;

import io.mango.common.result.Require;
import io.mango.resource.api.enums.ResourceCode;

/**
 * Resource module assertions kept compatible with published Mango Common artifacts.
 */
public final class ResourceRegistryAssertions {

    private ResourceRegistryAssertions() {
    }

    public static void requireNull(Object value, ResourceCode code, String message) {
        Require.isNull(value, code.getCode(), message);
    }
}
