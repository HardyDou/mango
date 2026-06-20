package io.mango.authorization.resource.sync.gateway;

import io.mango.authorization.resource.sync.ApiResourceDeclarationConverter;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.model.ResourceDeclaration;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Provides Gateway route API resources to Resource Registry.
 */
@RequiredArgsConstructor
public class GatewayRouteResourceProvider implements ResourceProvider {

    private final GatewayRouteResourceDiscoverer discoverer;
    private final ApiResourceDeclarationConverter converter;
    private final String moduleCode;

    @Override
    public List<String> moduleCodes() {
        return List.of(moduleCode);
    }

    @Override
    public List<ResourceDeclaration> provide() {
        return discoverer.discover().stream()
                .map(command -> converter.toDeclaration(command, moduleCode))
                .toList();
    }
}
