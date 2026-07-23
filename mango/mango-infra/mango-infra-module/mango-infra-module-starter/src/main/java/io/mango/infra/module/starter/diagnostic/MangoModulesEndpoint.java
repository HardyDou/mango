package io.mango.infra.module.starter.diagnostic;

import io.mango.infra.module.api.diagnostic.ModuleDiagnosticProfile;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticSnapshot;
import io.mango.infra.module.api.diagnostic.ModuleInstallationRegistry;
import io.mango.infra.module.core.diagnostic.ModuleDiagnosticCoordinator;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.List;

/** Read-only Actuator endpoint for one explicitly requested module. */
@Endpoint(id = "mangoModules")
public class MangoModulesEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(MangoModulesEndpoint.class);
    private static final int MAX_CODE_LENGTH = 80;

    private final ModuleDiagnosticCoordinator coordinator;
    private final ModuleInstallationRegistry installations;
    private final String service;
    private final String instanceId;

    public MangoModulesEndpoint(
            ModuleDiagnosticCoordinator coordinator,
            ModuleInstallationRegistry installations,
            String service,
            String instanceId) {
        this.coordinator = coordinator;
        this.installations = installations;
        this.service = service;
        this.instanceId = instanceId;
    }

    @ReadOperation
    public ModuleDiagnosticSnapshot diagnose(String module, String app, String profile) {
        long startedNanos = System.nanoTime();
        String moduleCode = requireCode(module, "module");
        String appCode = requireCode(app, "app");
        ModuleDiagnosticProfile selectedProfile = ModuleDiagnosticProfile.resolve(profile);
        ModuleDiagnosticRequest request = new ModuleDiagnosticRequest(
                moduleCode,
                appCode,
                selectedProfile,
                installations.resolve(moduleCode)
                        .map(installation -> installation.attributes())
                        .orElseGet(java.util.Map::of));
        ModuleDiagnosticSnapshot snapshot = new ModuleDiagnosticSnapshot(
                ModuleDiagnosticSnapshot.CURRENT_SCHEMA_VERSION,
                selectedProfile.name(),
                "INSTANCE_OBSERVATION",
                service,
                instanceId,
                Instant.now(),
                List.of(coordinator.diagnose(request)));
        LOG.info(
                "module_diagnostic_completed module={} app={} status={} durationMs={} requestId={}",
                moduleCode,
                appCode,
                snapshot.modules().get(0).status(),
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos),
                firstText(MDC.get("requestId"), "unknown"));
        return snapshot;
    }

    private String requireCode(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_CODE_LENGTH
                || !normalized.matches("[a-z0-9][a-z0-9-]*")) {
            throw new IllegalArgumentException(name + " must be a bounded lowercase code");
        }
        return normalized;
    }

    private String firstText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
