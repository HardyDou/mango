package io.mango.infra.feign.starter;

import io.mango.infra.module.api.ModuleInfo;
import io.mango.infra.module.api.ModuleInfoResolver;
import org.springframework.beans.factory.ObjectProvider;

import java.net.URI;
import java.util.Optional;

/**
 * Resolves Mango module names to real service targets for Feign calls.
 */
public class ModuleTargetResolver {

    private final ObjectProvider<ModuleInfoResolver> resolverProvider;

    public ModuleTargetResolver(ObjectProvider<ModuleInfoResolver> resolverProvider) {
        this.resolverProvider = resolverProvider;
    }

    public ModuleTargetResolver(ModuleInfoResolver resolver) {
        this.resolverProvider = new StaticModuleInfoResolverProvider(resolver);
    }

    /**
     * Resolve the deployment service URI of a Mango module.
     *
     * @param moduleName Mango module name.
     * @return service URI if module metadata exists.
     */
    public Optional<URI> resolveServiceUri(String moduleName) {
        if (moduleName == null || moduleName.isBlank()) {
            return Optional.empty();
        }
        ModuleInfoResolver resolver = resolverProvider.getIfAvailable();
        if (resolver == null) {
            return Optional.empty();
        }
        return resolver.resolve(moduleName.trim())
                .map(ModuleInfo::serviceName)
                .map(serviceName -> URI.create("http://" + serviceName));
    }

    private static class StaticModuleInfoResolverProvider implements ObjectProvider<ModuleInfoResolver> {

        private final ModuleInfoResolver resolver;

        StaticModuleInfoResolverProvider(ModuleInfoResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public ModuleInfoResolver getObject(Object... args) {
            return resolver;
        }

        @Override
        public ModuleInfoResolver getIfAvailable() {
            return resolver;
        }

        @Override
        public ModuleInfoResolver getIfUnique() {
            return resolver;
        }

        @Override
        public ModuleInfoResolver getObject() {
            return resolver;
        }
    }
}
