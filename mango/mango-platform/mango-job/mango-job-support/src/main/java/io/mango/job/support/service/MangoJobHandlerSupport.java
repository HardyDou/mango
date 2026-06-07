package io.mango.job.support.service;

import io.mango.common.result.Require;
import org.springframework.util.StringUtils;

/**
 * Job handler registry support methods.
 */
final class MangoJobHandlerSupport {

    private MangoJobHandlerSupport() {
    }

    static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    static String normalizeRequired(String value, String message) {
        Require.notBlank(value, message);
        return value.trim();
    }
}
