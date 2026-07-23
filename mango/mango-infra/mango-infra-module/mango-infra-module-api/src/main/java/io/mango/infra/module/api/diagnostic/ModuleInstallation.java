package io.mango.infra.module.api.diagnostic;

import io.mango.common.contract.LocalCapabilityContract;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime installation facts loaded from one module metadata resource.
 *
 * @param moduleCode stable module code
 * @param actualVersion loaded artifact version, or {@code null}
 * @param versionSource source of the actual version
 * @param attributes optional explicit diagnostic mappings
 */
@LocalCapabilityContract
public record ModuleInstallation(
        String moduleCode,
        String actualVersion,
        String versionSource,
        Map<String, String> attributes) {

    public static final String PERSISTENCE_MODULE_ATTRIBUTE = "persistenceModule";
    public static final String RESOURCE_MODULE_ATTRIBUTE = "resourceModule";

    public ModuleInstallation {
        if (moduleCode == null || moduleCode.isBlank()) {
            throw new IllegalArgumentException("moduleCode must not be blank");
        }
        moduleCode = moduleCode.trim();
        if (actualVersion != null && actualVersion.isBlank()) {
            actualVersion = null;
        }
        versionSource = versionSource == null || versionSource.isBlank()
                ? "UNKNOWN"
                : versionSource.trim();
        attributes = attributes == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    @Override
    public Map<String, String> attributes() {
        return Map.copyOf(attributes);
    }
}
