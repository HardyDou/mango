package io.mango.infra.module.api.diagnostic;

import io.mango.common.contract.LocalCapabilityContract;

import java.util.Collection;
import java.util.Optional;

/**
 * Read-only registry of modules actually observed on the current classpath.
 */
@LocalCapabilityContract
public interface ModuleInstallationRegistry {

    void register(ModuleInstallation installation);

    Optional<ModuleInstallation> resolve(String moduleCode);

    Collection<ModuleInstallation> list();
}
