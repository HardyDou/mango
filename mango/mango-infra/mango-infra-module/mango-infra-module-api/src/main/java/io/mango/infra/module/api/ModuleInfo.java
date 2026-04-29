package io.mango.infra.module.api;

/**
 * 当前服务中已部署 Mango 模块的运行时信息。
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

    /**
     * 模块在当前服务中的实际访问根路径，等于 contextPath + modulePath。
     */
    public String runtimeBasePath() {
        if (contextPath.isEmpty()) {
            return modulePath;
        }
        return contextPath + modulePath;
    }

    /**
     * 判断请求路径是否属于当前模块。
     */
    public boolean matchesRequestPath(String requestPath) {
        String normalized = normalizePath(requestPath);
        return matchesPath(normalized, modulePath) || matchesPath(normalized, runtimeBasePath());
    }

    private static boolean matchesPath(String path, String basePath) {
        return path.equals(basePath) || path.startsWith(basePath + "/");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
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
