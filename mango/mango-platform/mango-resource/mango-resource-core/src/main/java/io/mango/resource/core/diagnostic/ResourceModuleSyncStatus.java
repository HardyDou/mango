package io.mango.resource.core.diagnostic;

import java.time.Instant;
import java.util.List;

/**
 * Safe in-process evidence tying a Resource module to the current declaration fingerprint.
 */
public record ResourceModuleSyncStatus(
        String moduleCode,
        ResourceModuleSyncState state,
        String fingerprint,
        int declarationCount,
        int consumerResolvedCount,
        int targetEvidenceCount,
        List<String> pageRequirements,
        Instant observedAt,
        String reasonCode) {

    public ResourceModuleSyncStatus {
        moduleCode = requireText(moduleCode, "moduleCode");
        state = state == null ? ResourceModuleSyncState.UNKNOWN : state;
        fingerprint = fingerprint == null || fingerprint.isBlank() ? "unknown" : fingerprint.trim();
        declarationCount = Math.max(0, declarationCount);
        consumerResolvedCount = Math.max(0, consumerResolvedCount);
        targetEvidenceCount = Math.max(0, targetEvidenceCount);
        pageRequirements = pageRequirements == null ? List.of() : pageRequirements.stream().distinct().sorted().toList();
        observedAt = observedAt == null ? Instant.now() : observedAt;
        reasonCode = requireText(reasonCode, "reasonCode");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
