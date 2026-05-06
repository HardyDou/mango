package io.mango.authorization.resource.sync.gateway;

import io.mango.authorization.api.ApiResourceApi;
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
@ConditionalOnClass(name = "org.springframework.cloud.gateway.route.RouteDefinitionLocator")
public class GatewayRouteResourceSyncAutoConfiguration {

    @Bean
    @ConditionalOnBean({ApiResourceApi.class, RouteDefinitionLocator.class})
    @ConditionalOnMissingBean
    public GatewayRouteResourceSyncRunner gatewayRouteResourceSyncRunner(
            RouteDefinitionLocator routeDefinitionLocator,
            ApiResourceApi apiResourceApi) {
        return new GatewayRouteResourceSyncRunner(routeDefinitionLocator, apiResourceApi);
    }
}
