package io.mango.infra.module.api;

import io.mango.common.contract.LocalCapabilityContract;

/**
 * 当前服务中已部署 Mango 模块的运行时信息。
 */
@LocalCapabilityContract
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
        if (source == null || source.isBlank()) {
            source = "unknown";
        } else {
            source = source.trim();
        }
    }

    /**
     * 模块在当前服务中的实际访问根路径，等于 contextPath + modulePath。
     */
    public String runtimeBasePath() {
        if (contextPath.isEmpty()) {
            return modulePath;
        }
        if ("/".equals(modulePath)) {
            return contextPath;
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
        if ("/".equals(basePath)) {
            return path.startsWith("/");
        }
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
        return normalizeAbsolutePath(value);
    }

    private static String normalizeContextPath(String value) {
        if (value == null || value.isBlank() || "/".equals(value.trim())) {
            return "";
        }
        return normalizeAbsolutePath(value);
    }

    private static String normalizeModulePath(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("modulePath must not be blank");
        }
        return normalizeAbsolutePath(value);
    }

    private static String normalizeAbsolutePath(String value) {
        String normalized = value.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
