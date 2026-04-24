package io.mango.infra.module.api;

/**
 * Runtime information for a Mango module deployed in the current service.
 */
public record ModuleInfo(
        String moduleName,
        String serviceName,
        String contextPath,
        String modulePath,
        String source) {

    public ModuleInfo {
        moduleName = requireText(moduleName, "moduleName");
        serviceName = requireText(serviceName, "serviceName");
        contextPath = normalizeContextPath(contextPath);
        modulePath = normalizeModulePath(modulePath);
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

    private static String normalizeModulePath(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("modulePath must not be blank");
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        if (trimmed.length() > 1 && trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
