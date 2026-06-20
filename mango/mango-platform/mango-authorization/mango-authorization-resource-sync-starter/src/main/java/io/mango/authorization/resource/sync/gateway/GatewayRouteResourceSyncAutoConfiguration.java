package io.mango.authorization.resource.sync.gateway;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.resource.sync.ApiResourceDeclarationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;

/**
 * Spring Cloud Gateway 路由资源扫描自动配置。
 *
 * @author hardy
 */
@AutoConfiguration
@ConditionalOnProperty(name = "mango.authorization.resource-sync.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(name = "mango.authorization.resource-sync.gateway.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.route.RouteDefinitionLocator")
public class GatewayRouteResourceSyncAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = "io.mango.resource.api.ResourceProvider")
    @ConditionalOnMissingBean
    public ApiResourceDeclarationConverter apiResourceDeclarationConverter() {
        return new ApiResourceDeclarationConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public GatewayRouteResourceDiscoverer gatewayRouteResourceDiscoverer(
            RouteDefinitionLocator routeDefinitionLocator,
            @Value("${mango.authorization.resource-sync.gateway.module-name:gateway}") String moduleName) {
        return new GatewayRouteResourceDiscoverer(routeDefinitionLocator, moduleName);
    }

    @Bean
    @ConditionalOnClass(name = "io.mango.resource.api.ResourceProvider")
    @ConditionalOnBean(ApiResourceDeclarationConverter.class)
    @ConditionalOnProperty(name = "mango.authorization.resource-sync.gateway.mode", havingValue = "write")
    @ConditionalOnMissingBean
    public GatewayRouteResourceProvider gatewayRouteResourceProvider(
            GatewayRouteResourceDiscoverer discoverer,
            ApiResourceDeclarationConverter converter,
            @Value("${mango.authorization.resource-sync.gateway.module-name:gateway}") String moduleName) {
        return new GatewayRouteResourceProvider(discoverer, converter, moduleName);
    }

    @Bean
    @ConditionalOnBean({ApiResourceApi.class, GatewayRouteResourceDiscoverer.class})
    @ConditionalOnProperty(name = "mango.authorization.resource-sync.legacy-writer-enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public GatewayRouteResourceSyncRunner gatewayRouteResourceSyncRunner(
            GatewayRouteResourceDiscoverer discoverer,
            ApiResourceApi apiResourceApi,
            @Value("${mango.authorization.resource-sync.gateway.mode:read}") String syncMode) {
        return new GatewayRouteResourceSyncRunner(discoverer, apiResourceApi, syncMode);
    }
}
