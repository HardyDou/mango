package io.mango.infra.module.starter;

import io.mango.infra.module.api.diagnostic.ModuleConditionStatus;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticCondition;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticContributor;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticProfile;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import io.mango.infra.module.api.diagnostic.ModuleInstallation;
import io.mango.infra.module.api.diagnostic.ModuleInstallationRegistry;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reports actual classpath installation and version evidence.
 */
public class ModuleInstallationDiagnosticContributor implements ModuleDiagnosticContributor {

    private final ModuleInstallationRegistry registry;

    public ModuleInstallationDiagnosticContributor(ModuleInstallationRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Collection<ModuleDiagnosticCondition> contribute(ModuleDiagnosticRequest request) {
        Instant observedAt = Instant.now();
        return registry.resolve(request.moduleCode())
                .map(installation -> installed(installation, observedAt))
                .map(List::of)
                .orElseGet(() -> List.of(notObserved(request, observedAt)));
    }

    private ModuleDiagnosticCondition notObserved(ModuleDiagnosticRequest request, Instant observedAt) {
        boolean authoritativeOwner = ModuleDiagnosticRequest.AUTHORITATIVE_OWNER_SCOPE.equals(
                request.attributes().get(ModuleDiagnosticRequest.REPORT_SCOPE_ATTRIBUTE));
        return new ModuleDiagnosticCondition(
                ModuleDiagnosticProfile.INSTALLATION,
                authoritativeOwner ? ModuleConditionStatus.FAIL : ModuleConditionStatus.UNKNOWN,
                true,
                authoritativeOwner ? "MODULE_NOT_INSTALLED" : "MODULE_NOT_OBSERVED_ON_INSTANCE",
                Map.of("reportScope", authoritativeOwner
                        ? ModuleDiagnosticRequest.AUTHORITATIVE_OWNER_SCOPE
                        : ModuleDiagnosticRequest.INSTANCE_OBSERVATION_SCOPE),
                observedAt,
                0,
                false);
    }

    private ModuleDiagnosticCondition installed(ModuleInstallation installation, Instant observedAt) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("installed", true);
        if (installation.actualVersion() != null) {
            evidence.put("backendVersion", installation.actualVersion());
            evidence.put("versionSource", installation.versionSource());
        }
        String reason = installation.actualVersion() == null
                ? "MODULE_INSTALLED_VERSION_UNKNOWN"
                : "MODULE_INSTALLED";
        return new ModuleDiagnosticCondition(
                ModuleDiagnosticProfile.INSTALLATION,
                ModuleConditionStatus.PASS,
                true,
                reason,
                evidence,
                observedAt,
                0,
                false);
    }
}
