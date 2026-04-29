package io.mango.infra.module.api;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * 模块部署信息注册表。
 */
public interface ModuleInfoRegistry {

    void register(ModuleInfo moduleInfo);

    Optional<ModuleInfo> resolve(String moduleName);

    Collection<ModuleInfo> list();

    /**
     * 按模块路径解析模块，适合 Controller / API 资源扫描时使用。
     */
    default Optional<ModuleInfo> resolveByModulePath(String modulePath) {
        if (modulePath == null || modulePath.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalizePath(modulePath);
        return list().stream()
                .filter(moduleInfo -> moduleInfo.modulePath().equals(normalized))
                .findFirst();
    }

    /**
     * 按请求路径反查模块，优先返回路径最长的模块，避免 /system 抢占 /system/config。
     */
    default Optional<ModuleInfo> resolveByRequestPath(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return Optional.empty();
        }
        return list().stream()
                .filter(moduleInfo -> moduleInfo.matchesRequestPath(requestPath))
                .max(Comparator.comparingInt(moduleInfo -> moduleInfo.modulePath().length()));
    }

    private static String normalizePath(String value) {
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
