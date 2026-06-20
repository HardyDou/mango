package io.mango.authorization.resource.sync;

import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.model.ResourceDeclaration;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 将 Spring MVC API 访问声明提供给 mango-resource。
 */
@RequiredArgsConstructor
public class ApiAccessResourceProvider implements ResourceProvider {

    private final ApiAccessResourceDiscoverer discoverer;
    private final ApiResourceSyncProperties properties;
    private final ApiResourceDeclarationConverter converter;

    @Override
    public List<String> moduleCodes() {
        return List.of(properties.getProviderModuleCode());
    }

    @Override
    public List<ResourceDeclaration> provide() {
        return discoverer.discover().stream()
                .map(command -> converter.toDeclaration(command, properties.getProviderModuleCode()))
                .toList();
    }
}
