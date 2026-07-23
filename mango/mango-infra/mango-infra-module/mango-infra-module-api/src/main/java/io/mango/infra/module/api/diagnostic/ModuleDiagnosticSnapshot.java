package io.mango.infra.module.api.diagnostic;

import io.mango.common.contract.LocalCapabilityContract;

import java.time.Instant;
import java.util.List;

/**
 * Versioned diagnostic response produced by one runtime observation boundary.
 */
@LocalCapabilityContract
public record ModuleDiagnosticSnapshot(
        int schemaVersion,
        String profile,
        String reportScope,
        String service,
        String instanceId,
        Instant observedAt,
        List<ModuleDiagnosticReport> modules) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public ModuleDiagnosticSnapshot {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported schemaVersion");
        }
        reportScope = normalize(reportScope, "INSTANCE_OBSERVATION");
        service = normalize(service, "application");
        instanceId = normalize(instanceId, "unknown");
        observedAt = observedAt == null ? Instant.now() : observedAt;
        modules = modules == null ? List.of() : List.copyOf(modules);
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
