package io.mango.authorization.resource.sync.gateway;

import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Discovers API resource definitions from Spring Cloud Gateway route Path predicates.
 */
@Slf4j
@RequiredArgsConstructor
public class GatewayRouteResourceDiscoverer {

    private final RouteDefinitionLocator routeDefinitionLocator;
    private final String moduleName;

    public List<ApiResourceRegisterCommand> discover() {
        List<RouteDefinition> routeDefinitions = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        if (routeDefinitions == null || routeDefinitions.isEmpty()) {
            return List.of();
        }
        List<ApiResourceRegisterCommand> resources = new ArrayList<>();
        for (RouteDefinition routeDefinition : routeDefinitions) {
            for (String path : resolvePathPatterns(routeDefinition)) {
                resources.add(toResource(routeDefinition, path));
            }
        }
        resources.sort(Comparator
                .comparing(ApiResourceRegisterCommand::getPathPattern)
                .thenComparing(ApiResourceRegisterCommand::getResourceCode));
        return resources;
    }

    private List<String> resolvePathPatterns(RouteDefinition routeDefinition) {
        List<String> paths = new ArrayList<>();
        for (PredicateDefinition predicate : routeDefinition.getPredicates()) {
            if (!"Path".equalsIgnoreCase(predicate.getName())) {
                continue;
            }
            for (String value : predicate.getArgs().values()) {
                for (String path : value.split(",")) {
                    String trimmed = path.trim();
                    if (StringUtils.hasText(trimmed)) {
                        paths.add(trimmed);
                    }
                }
            }
        }
        return paths;
    }

    private ApiResourceRegisterCommand toResource(RouteDefinition routeDefinition, String path) {
        ApiResourceRegisterCommand resource = new ApiResourceRegisterCommand();
        resource.setModuleName(moduleName);
        resource.setHttpMethod("ALL");
        resource.setPathPattern(path);
        resource.setResourceCode("GATEWAY:" + path);
        resource.setAccessMode(resolveAccessMode(routeDefinition));
        resource.setHandlerClass("SpringCloudGateway");
        resource.setHandlerMethod(routeDefinition.getId());
        resource.setDescription("Gateway route " + routeDefinition.getId() + " -> " + routeDefinition.getUri());
        return resource;
    }

    private ApiResourceAccessMode resolveAccessMode(RouteDefinition routeDefinition) {
        Map<String, Object> metadata = routeDefinition.getMetadata();
        if (metadata == null) {
            return ApiResourceAccessMode.LOGIN;
        }
        Object accessMode = metadata.get("apiAccessMode");
        if (accessMode == null) {
            return ApiResourceAccessMode.LOGIN;
        }
        try {
            return ApiResourceAccessMode.valueOf(accessMode.toString());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid gateway route apiAccessMode: routeId={}, value={}",
                    routeDefinition.getId(), accessMode);
            return ApiResourceAccessMode.LOGIN;
        }
    }
}
