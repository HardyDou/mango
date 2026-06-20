package io.mango.infra.module.core;

import io.mango.infra.module.api.ModuleInfo;
import io.mango.infra.module.api.ModuleInfoRegistry;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存模块信息注册表。
 */
public class MemoryModuleInfoRegistry implements ModuleInfoRegistry {

    private final Map<String, ModuleInfo> modules = new ConcurrentHashMap<>();
    private final Map<String, ModuleInfo> modulePaths = new ConcurrentHashMap<>();

    @Override
    public void register(ModuleInfo moduleInfo) {
        modules.putIfAbsent(moduleInfo.moduleName(), moduleInfo);
        modulePaths.put(moduleInfo.moduleName() + "\n" + moduleInfo.modulePath(), moduleInfo);
    }

    @Override
    public Optional<ModuleInfo> resolve(String moduleName) {
        if (moduleName == null || moduleName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(modules.get(moduleName.trim()));
    }

    @Override
    public Collection<ModuleInfo> list() {
        List<ModuleInfo> additionalPaths = modulePaths.values().stream()
                .filter(moduleInfo -> !moduleInfo.equals(modules.get(moduleInfo.moduleName())))
                .toList();
        return java.util.stream.Stream.concat(modules.values().stream(), additionalPaths.stream())
                .sorted(Comparator.comparing(ModuleInfo::moduleName))
                .toList();
    }
}
