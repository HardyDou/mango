package io.mango.infra.module.api.diagnostic;

import io.mango.common.contract.LocalCapabilityContract;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Request-scoped set of evidence required to make an aggregate diagnosis.
 * This is not a persisted desired-state manifest.
 *
 * @param name stable profile name
 * @param requiredConditionIds required condition identifiers
 */
@LocalCapabilityContract
public record ModuleDiagnosticProfile(String name, Set<String> requiredConditionIds) {

    public static final String INSTALLATION = "installation";
    public static final String PERSISTENCE_FLYWAY = "persistence.flyway";
    public static final String RESOURCE_MATERIALIZATION = "resource.materialization";
    public static final String AUTHORIZATION_MENU_API = "authorization.menuApi";
    public static final String FRONTEND_PAGE_RUNTIME = "frontend.pageRuntime";

    /** Complete first-version Admin module runtime profile. */
    public static final ModuleDiagnosticProfile ADMIN_MODULE_RUNTIME_V1 = new ModuleDiagnosticProfile(
            "ADMIN_MODULE_RUNTIME_V1",
            Set.of(
                    INSTALLATION,
                    PERSISTENCE_FLYWAY,
                    RESOURCE_MATERIALIZATION,
                    AUTHORIZATION_MENU_API,
                    FRONTEND_PAGE_RUNTIME));

    /** Basic observed installation profile for callers that do not claim full readiness. */
    public static final ModuleDiagnosticProfile INSTALLATION_ONLY = new ModuleDiagnosticProfile(
            "INSTALLATION_ONLY",
            Set.of(INSTALLATION));

    public ModuleDiagnosticProfile {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        name = name.trim();
        if (requiredConditionIds == null || requiredConditionIds.isEmpty()) {
            throw new IllegalArgumentException("requiredConditionIds must not be empty");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        requiredConditionIds.forEach(conditionId -> {
            if (conditionId == null || conditionId.isBlank()) {
                throw new IllegalArgumentException("required condition id must not be blank");
            }
            normalized.add(conditionId.trim());
        });
        requiredConditionIds = Set.copyOf(normalized);
    }

    /**
     * Resolves a supported built-in profile.
     */
    public static ModuleDiagnosticProfile resolve(String name) {
        if (name == null || name.isBlank() || INSTALLATION_ONLY.name().equals(name)) {
            return INSTALLATION_ONLY;
        }
        if (ADMIN_MODULE_RUNTIME_V1.name().equals(name)) {
            return ADMIN_MODULE_RUNTIME_V1;
        }
        throw new IllegalArgumentException("Unsupported module diagnostic profile: " + name);
    }
}
