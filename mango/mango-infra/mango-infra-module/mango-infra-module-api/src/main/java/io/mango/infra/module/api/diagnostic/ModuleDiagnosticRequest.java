package io.mango.infra.module.api.diagnostic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable explicit scope passed to diagnostic contributors.
 *
 * @param moduleCode requested module
 * @param appCode requested application
 * @param profile selected request profile
 * @param attributes safe module mapping attributes resolved before asynchronous execution
 */
public record ModuleDiagnosticRequest(
        String moduleCode,
        String appCode,
        ModuleDiagnosticProfile profile,
        Map<String, String> attributes) {

    /** Optional request attribute proving that the queried runtime is the module's complete owner. */
    public static final String REPORT_SCOPE_ATTRIBUTE = "reportScope";
    public static final String AUTHORITATIVE_OWNER_SCOPE = "AUTHORITATIVE_OWNER";
    public static final String INSTANCE_OBSERVATION_SCOPE = "INSTANCE_OBSERVATION";

    public ModuleDiagnosticRequest {
        moduleCode = requireText(moduleCode, "moduleCode");
        appCode = requireText(appCode, "appCode");
        profile = profile == null ? ModuleDiagnosticProfile.INSTALLATION_ONLY : profile;
        attributes = attributes == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
