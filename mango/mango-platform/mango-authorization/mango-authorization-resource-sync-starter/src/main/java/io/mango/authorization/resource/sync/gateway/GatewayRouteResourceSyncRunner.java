package io.mango.authorization.resource.sync.gateway;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 同步 Spring Cloud Gateway 路由暴露面到授权资源表。
 */
@Slf4j
public class GatewayRouteResourceSyncRunner implements ApplicationRunner {

    private final RouteDefinitionLocator routeDefinitionLocator;
    private final ApiResourceApi apiResourceApi;

    @Value("${mango.authorization.resource-sync.gateway.module-name:gateway}")
    private String moduleName = "gateway";

    @Value("${mango.authorization.resource-sync.gateway.mode:write}")
    private String syncMode;

    @Value("${mango.authorization.resource-sync.gateway.enabled:true}")
    private boolean enabled;

    public GatewayRouteResourceSyncRunner(
            RouteDefinitionLocator routeDefinitionLocator,
            ApiResourceApi apiResourceApi) {
        this.routeDefinitionLocator = routeDefinitionLocator;
        this.apiResourceApi = apiResourceApi;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Gateway route resource sync disabled");
            return;
        }
        List<ApiResourceRegisterCommand> resources = scanRoutes();
        if (resources.isEmpty()) {
            log.info("Gateway route resource sync skipped: no Path routes discovered");
            return;
        }
        if ("read".equalsIgnoreCase(syncMode)) {
            log.info("Gateway route resource sync read-only: discovered {} resources", resources.size());
            return;
        }
        R<ApiResourceRegisterResultVO> response = apiResourceApi.registerApiResources(resources);
        ApiResourceRegisterResultVO result = response != null && response.isSuccess() && response.getData() != null
                ? response.getData()
                : ApiResourceRegisterResultVO.empty();
        log.info("Gateway route resource sync complete: scanned={}, created={}, updated={}",
                result.scanned(), result.created(), result.updated());
    }

    List<ApiResourceRegisterCommand> scanRoutes() {
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
