package io.mango.infra.module.api;

/**
 * Runtime information for a Mango module deployed in the current service.
 */
public record ModuleInfo(
        String moduleName,
        String serviceName,
        String contextPath,
        String source) {

    public ModuleInfo {
        moduleName = requireText(moduleName, "moduleName");
        serviceName = requireText(serviceName, "serviceName");
        contextPath = normalizeContextPath(contextPath);
        source = source == null || source.isBlank() ? "unknown" : source.trim();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeContextPath(String value) {
        if (value == null || value.isBlank() || "/".equals(value.trim())) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}
