package io.mango.infra.module.api;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry for module deployment information.
 */
public interface ModuleInfoRegistry {

    void register(ModuleInfo moduleInfo);

    Optional<ModuleInfo> resolve(String moduleName);

    Collection<ModuleInfo> list();
}
