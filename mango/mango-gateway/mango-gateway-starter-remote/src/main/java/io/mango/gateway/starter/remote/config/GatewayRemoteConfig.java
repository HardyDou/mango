package io.mango.gateway.starter.remote.config;

import io.mango.gateway.core.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway远程配置 (微服务模式)
 * <p>
 * 当 use-discovery=true 时启用，提供服务发现路由
 *
 * @author Mango
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mango.gateway.routes.use-discovery", havingValue = "true")
public class GatewayRemoteConfig {

    private final GatewayProperties properties;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        GatewayProperties.Routes routes = properties.getRoutes();

        return builder.routes()
                // 认证服务
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://" + routes.getAuthService()))
                // BFF Admin服务
                .route("bff-admin-service", r -> r
                        .path("/bff/admin/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://" + routes.getBffAdminService()))
                // AI服务
                .route("ai-service", r -> r
                        .path("/ai/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://" + routes.getAiService()))
                // 用户服务
                .route("user-service", r -> r
                        .path("/user/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://" + routes.getUserService()))
                // I18n服务
                .route("i18n-service", r -> r
                        .path("/i18n/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://" + routes.getI18nService()))
                // 行政区划服务
                .route("area-service", r -> r
                        .path("/area/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://" + routes.getAreaService()))
                .build();
    }
}
