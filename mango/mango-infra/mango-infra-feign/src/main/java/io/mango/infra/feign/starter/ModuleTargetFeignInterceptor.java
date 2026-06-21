package io.mango.infra.feign.starter;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.mango.infra.module.api.ModuleInfo;
import io.mango.infra.module.api.ModuleInfoResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;

import java.net.URI;
import java.util.Optional;

/**
 * Rewrites Feign module targets to real deployment service targets.
 */
public class ModuleTargetFeignInterceptor implements RequestInterceptor, Ordered {

    public static final int ORDER = 0;

    private final ObjectProvider<ModuleInfoResolver> resolverProvider;

    public ModuleTargetFeignInterceptor(ObjectProvider<ModuleInfoResolver> resolverProvider) {
        this.resolverProvider = resolverProvider;
    }

    ModuleTargetFeignInterceptor(ModuleInfoResolver resolver) {
        this.resolverProvider = new StaticModuleInfoResolverProvider(resolver);
    }

    @Override
    public void apply(RequestTemplate template) {
        if (template.feignTarget() == null) {
            return;
        }
        if (isAbsoluteUrl(template.url())) {
            return;
        }
        String moduleName = template.feignTarget().name();
        if (moduleName == null || moduleName.isBlank()) {
            return;
        }
        ModuleInfoResolver resolver = resolverProvider.getIfAvailable();
        if (resolver == null) {
            return;
        }
        Optional<ModuleInfo> moduleInfo = resolver.resolve(moduleName);
        if (moduleInfo.isEmpty()) {
            return;
        }

        ModuleInfo info = moduleInfo.get();
        String originalPath = extractPath(template.path());
        template.target("http://" + info.serviceName());
        template.uri(joinPath(info.contextPath(), originalPath), false);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private String extractPath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return value;
        }
        return URI.create(value).getRawPath();
    }

    private boolean isAbsoluteUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    private String joinPath(String contextPath, String path) {
        String normalizedContextPath = normalize(contextPath);
        String normalizedPath = normalize(path);
        if (normalizedContextPath.isEmpty()) {
            return normalizedPath.isEmpty() ? "/" : normalizedPath;
        }
        if (normalizedPath.isEmpty() || "/".equals(normalizedPath)) {
            return normalizedContextPath;
        }
        return normalizedContextPath + normalizedPath;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank() || "/".equals(value.trim())) {
            return "";
        }
        String trimmed = value.trim();
        String withLeadingSlash = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return withLeadingSlash.endsWith("/") ? withLeadingSlash.substring(0, withLeadingSlash.length() - 1) : withLeadingSlash;
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
