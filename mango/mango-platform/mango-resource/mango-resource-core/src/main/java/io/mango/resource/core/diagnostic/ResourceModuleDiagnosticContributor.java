package io.mango.resource.core.diagnostic;

import io.mango.infra.module.api.diagnostic.ModuleConditionStatus;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticCondition;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticContributor;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticProfile;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import io.mango.infra.module.api.diagnostic.ModuleInstallation;
import io.mango.resource.support.config.ResourceRegistryProperties;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maps current Resource synchronization evidence to a neutral module condition. */
public class ResourceModuleDiagnosticContributor implements ModuleDiagnosticContributor {

    private final ResourceModuleSyncStatusRegistry registry;
    private final boolean enabled;

    public ResourceModuleDiagnosticContributor(
            ResourceModuleSyncStatusRegistry registry,
            ResourceRegistryProperties properties) {
        this.registry = registry;
        this.enabled = properties.isEnabled();
    }

    @Override
    public Collection<ModuleDiagnosticCondition> contribute(ModuleDiagnosticRequest request) {
        String resourceModule = request.attributes().get(ModuleInstallation.RESOURCE_MODULE_ATTRIBUTE);
        if (resourceModule == null || resourceModule.isBlank()) {
            return List.of(condition(ModuleConditionStatus.UNKNOWN, "MAPPING_UNRESOLVED", Map.of(), Instant.now()));
        }
        if (!enabled) {
            return List.of(condition(
                    ModuleConditionStatus.SKIPPED,
                    "RESOURCE_SYNC_DISABLED",
                    Map.of("resourceModule", resourceModule),
                    Instant.now()));
        }
        return registry.resolve(resourceModule)
                .map(this::toCondition)
                .map(List::of)
                .orElseGet(() -> List.of(condition(
                        ModuleConditionStatus.UNKNOWN,
                        "CURRENT_SYNC_NOT_OBSERVED",
                        Map.of("resourceModule", resourceModule),
                        Instant.now())));
    }

    private ModuleDiagnosticCondition toCondition(ResourceModuleSyncStatus status) {
        ModuleConditionStatus conditionStatus = switch (status.state()) {
            case APPLIED -> ModuleConditionStatus.PASS;
            case FAILED -> ModuleConditionStatus.FAIL;
            case RUNNING, UNKNOWN -> ModuleConditionStatus.UNKNOWN;
        };
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("resourceModule", status.moduleCode());
        evidence.put("declarationCount", status.declarationCount());
        evidence.put("consumerResolvedCount", status.consumerResolvedCount());
        evidence.put("targetEvidenceCount", status.targetEvidenceCount());
        evidence.put("fingerprint", status.fingerprint());
        evidence.put("pageRequirements", status.pageRequirements());
        return condition(conditionStatus, status.reasonCode(), evidence, status.observedAt());
    }

    private ModuleDiagnosticCondition condition(
            ModuleConditionStatus status,
            String reasonCode,
            Map<String, Object> evidence,
            Instant observedAt) {
        return new ModuleDiagnosticCondition(
                ModuleDiagnosticProfile.RESOURCE_MATERIALIZATION,
                status,
                true,
                reasonCode,
                evidence,
                observedAt,
                0,
                false);
    }
}
