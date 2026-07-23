package io.mango.infra.module.core.diagnostic;

import io.mango.infra.module.api.diagnostic.ModuleInstallation;
import io.mango.infra.module.api.diagnostic.ModuleInstallationRegistry;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-local installation registry.
 */
public class MemoryModuleInstallationRegistry implements ModuleInstallationRegistry {

    private final Map<String, ModuleInstallation> installations = new ConcurrentHashMap<>();

    @Override
    public void register(ModuleInstallation installation) {
        installations.putIfAbsent(installation.moduleCode(), installation);
    }

    @Override
    public Optional<ModuleInstallation> resolve(String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(installations.get(moduleCode.trim()));
    }

    @Override
    public Collection<ModuleInstallation> list() {
        return installations.values().stream()
                .sorted(Comparator.comparing(ModuleInstallation::moduleCode))
                .toList();
    }
}
