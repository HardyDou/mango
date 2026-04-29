package io.mango.infra.module.core;

import io.mango.infra.module.api.ModuleInfo;
import io.mango.infra.module.api.ModuleInfoRegistry;
import io.mango.infra.module.api.ModuleInfoResolver;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存模块信息注册表。
 */
public class MemoryModuleInfoRegistry implements ModuleInfoRegistry, ModuleInfoResolver {

    private final Map<String, ModuleInfo> modules = new ConcurrentHashMap<>();

    @Override
    public void register(ModuleInfo moduleInfo) {
        modules.put(moduleInfo.moduleName(), moduleInfo);
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
        return modules.values().stream()
                .sorted(Comparator.comparing(ModuleInfo::moduleName))
                .toList();
    }
}
