package io.mango.infra.module.api;

import java.util.Optional;

/**
 * Resolves real service information by Mango module name.
 */
@FunctionalInterface
public interface ModuleInfoResolver {

    Optional<ModuleInfo> resolve(String moduleName);
}
