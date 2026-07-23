package io.mango.infra.module.api.diagnostic;

import io.mango.common.contract.LocalCapabilityContract;

/**
 * Actual or expected version evidence for one runtime plane.
 *
 * @param value observed version, or {@code null} when unknown
 * @param source stable evidence source
 * @param status evidence status
 * @param reasonCode stable reason
 */
@LocalCapabilityContract
public record ModuleVersionEvidence(
        String value,
        String source,
        ModuleConditionStatus status,
        String reasonCode) {

    public ModuleVersionEvidence {
        source = normalize(source, "UNKNOWN");
        status = status == null ? ModuleConditionStatus.UNKNOWN : status;
        reasonCode = normalize(reasonCode, "VERSION_UNKNOWN");
        if (value != null && value.isBlank()) {
            value = null;
        }
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
