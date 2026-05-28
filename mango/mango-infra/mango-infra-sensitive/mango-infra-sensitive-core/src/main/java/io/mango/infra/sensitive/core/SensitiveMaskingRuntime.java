package io.mango.infra.sensitive.core;

import io.mango.infra.sensitive.api.ISensitiveMaskingService;

/**
 * Runtime bridge used by Jackson serializers.
 */
public final class SensitiveMaskingRuntime {

    private static final ISensitiveMaskingService DEFAULT_SERVICE = new DefaultSensitiveMaskingService();

    private static volatile ISensitiveMaskingService maskingService = DEFAULT_SERVICE;

    private SensitiveMaskingRuntime() {
    }

    /**
     * Returns the active masking service.
     *
     * @return masking service
     */
    public static ISensitiveMaskingService getMaskingService() {
        return maskingService;
    }

    /**
     * Installs the active masking service.
     *
     * @param service masking service
     */
    public static void setMaskingService(ISensitiveMaskingService service) {
        maskingService = service == null ? DEFAULT_SERVICE : service;
    }

    /**
     * Restores the default masking service.
     */
    public static void reset() {
        maskingService = DEFAULT_SERVICE;
    }
}
