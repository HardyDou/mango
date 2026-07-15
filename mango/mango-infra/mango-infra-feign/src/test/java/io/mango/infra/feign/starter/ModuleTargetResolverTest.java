package io.mango.infra.feign.starter;

import io.mango.infra.module.api.ModuleInfo;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleTargetResolverTest {

    @Test
    void resolveModuleBaseUri_moduleExists_returnsServiceAndContextPath() {
        ModuleTargetResolver resolver = new ModuleTargetResolver(moduleName -> Optional.of(
                new ModuleInfo(moduleName, "mango-platform-app", "admin", "/resource", "test")));

        Optional<URI> result = resolver.resolveModuleBaseUri(" mango-resource ");

        assertThat(result).contains(URI.create("http://mango-platform-app/admin"));
    }

    @Test
    void resolveModuleBaseUri_missingInputOrModule_returnsEmpty() {
        ModuleTargetResolver resolver = new ModuleTargetResolver(moduleName -> Optional.empty());

        assertThat(resolver.resolveModuleBaseUri(" ")).isEmpty();
        assertThat(resolver.resolveModuleBaseUri("mango-missing")).isEmpty();
    }
}
