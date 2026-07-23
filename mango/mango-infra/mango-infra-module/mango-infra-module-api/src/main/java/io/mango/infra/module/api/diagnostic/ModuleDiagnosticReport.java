package io.mango.infra.module.api.diagnostic;

import java.util.List;

/**
 * Aggregate diagnosis for one observed module.
 */
public record ModuleDiagnosticReport(
        String moduleCode,
        ModuleRuntimeStatus status,
        boolean incompleteOptional,
        ModuleVersionEvidence backendVersion,
        ModuleVersionEvidence frontendVersion,
        ModuleVersionEvidence expectedVersion,
        List<ModuleDiagnosticCondition> conditions) {

    public ModuleDiagnosticReport {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }
}
