package io.mango.authorization.resource.sync;

import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.model.ResourceDeclaration;

import java.util.List;

/**
 * 将 Spring MVC API 访问声明提供给 mango-resource。
 */
public class ApiAccessResourceProvider implements ResourceProvider {

    private final ApiAccessResourceDiscoverer discoverer;
    private final String providerModuleCode;
    private final ApiResourceDeclarationConverter converter;

    public ApiAccessResourceProvider(ApiAccessResourceDiscoverer discoverer,
                                     ApiResourceSyncProperties properties,
                                     ApiResourceDeclarationConverter converter) {
        this.discoverer = discoverer;
        this.providerModuleCode = properties.getProviderModuleCode();
        this.converter = converter;
    }

    @Override
    public boolean participatesInBootstrap() {
        return false;
    }

    @Override
    public List<String> moduleCodes() {
        return List.of(providerModuleCode);
    }

    @Override
    public List<ResourceDeclaration> provide() {
        return discoverer.discover().stream()
                .map(command -> converter.toDeclaration(command, providerModuleCode))
                .toList();
    }
}
