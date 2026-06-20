package io.mango.authorization.resource.sync.gateway;

import io.mango.authorization.resource.sync.ApiResourceDeclarationConverter;
import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.ApiResourceAccessDecisionQuery;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.common.result.R;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.assertj.core.api.Assertions.assertThat;

class GatewayRouteResourceSyncRunnerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GatewayRouteResourceSyncAutoConfiguration.class))
            .withBean(org.springframework.cloud.gateway.route.RouteDefinitionLocator.class, () -> () -> Flux.empty());

    @Test
    @DisplayName("scanRoutes should register every gateway path predicate as login exposure by default")
    void scanRoutes_shouldRegisterEveryGatewayPathPredicateAsLoginExposureByDefault() {
        RouteDefinition route = new RouteDefinition();
        route.setId("platform");
        route.setUri(URI.create("http://localhost:8081"));
        route.setPredicates(List.of(new PredicateDefinition("Path=/auth/**,/system/**")));

        GatewayRouteResourceSyncRunner runner = new GatewayRouteResourceSyncRunner(
                new GatewayRouteResourceDiscoverer(() -> Flux.just(route), "gateway"),
                new TestApi(),
                "write");

        List<ApiResourceRegisterCommand> resources = runner.scanRoutes();

        assertEquals(2, resources.size());
        ApiResourceRegisterCommand auth = resources.get(0);
        assertEquals("gateway", auth.getModuleName());
        assertEquals("ALL", auth.getHttpMethod());
        assertEquals("/auth/**", auth.getPathPattern());
        assertEquals("GATEWAY:/auth/**", auth.getResourceCode());
        assertEquals(ApiResourceAccessMode.LOGIN, auth.getAccessMode());
        assertNull(auth.getPermissionCode());
        assertEquals("SpringCloudGateway", auth.getHandlerClass());
        assertEquals("platform", auth.getHandlerMethod());

        ApiResourceRegisterCommand system = resources.get(1);
        assertEquals("/system/**", system.getPathPattern());
        assertEquals("GATEWAY:/system/**", system.getResourceCode());
        assertEquals(ApiResourceAccessMode.LOGIN, system.getAccessMode());
    }

    @Test
    @DisplayName("scanRoutes should allow route metadata to override access mode")
    void scanRoutes_shouldAllowRouteMetadataToOverrideAccessMode() {
        RouteDefinition route = new RouteDefinition();
        route.setId("captcha");
        route.setUri(URI.create("http://localhost:8082"));
        route.setPredicates(List.of(new PredicateDefinition("Path=/captcha/**")));
        route.setMetadata(Map.of("apiAccessMode", "PUBLIC"));

        GatewayRouteResourceSyncRunner runner = new GatewayRouteResourceSyncRunner(
                new GatewayRouteResourceDiscoverer(() -> Flux.just(route), "gateway"),
                new TestApi(),
                "write");

        List<ApiResourceRegisterCommand> resources = runner.scanRoutes();

        assertEquals(1, resources.size());
        assertEquals("/captcha/**", resources.get(0).getPathPattern());
        assertEquals(ApiResourceAccessMode.PUBLIC, resources.get(0).getAccessMode());
        assertNull(resources.get(0).getPermissionCode());
    }

    @Test
    @DisplayName("auto configuration should expose gateway routes as Resource Registry provider by default")
    void autoConfiguration_shouldExposeGatewayRoutesAsProviderByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GatewayRouteResourceProvider.class);
            assertThat(context).doesNotHaveBean(GatewayRouteResourceSyncRunner.class);
        });
    }

    @Test
    @DisplayName("legacy gateway writer should only be created when explicitly enabled")
    void legacyGatewayWriter_shouldOnlyBeCreatedWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("mango.authorization.resource-sync.legacy-writer-enabled=true")
                .withBean(ApiResourceApi.class, TestApi::new)
                .run(context -> assertThat(context).hasSingleBean(GatewayRouteResourceSyncRunner.class));
    }

    @Test
    @DisplayName("provider should convert gateway routes into API_RESOURCE declarations")
    void provider_shouldConvertGatewayRoutesIntoApiResourceDeclarations() {
        RouteDefinition route = new RouteDefinition();
        route.setId("platform");
        route.setUri(URI.create("http://localhost:8081"));
        route.setPredicates(List.of(new PredicateDefinition("Path=/auth/**")));

        GatewayRouteResourceProvider provider = new GatewayRouteResourceProvider(
                new GatewayRouteResourceDiscoverer(() -> Flux.just(route), "gateway"),
                new ApiResourceDeclarationConverter(),
                "gateway");

        List<ResourceDeclaration> declarations = provider.provide();

        assertThat(provider.moduleCodes()).containsExactly("gateway");
        assertThat(declarations).hasSize(1);
        ResourceDeclaration declaration = declarations.get(0);
        assertThat(declaration.getResourceType()).isEqualTo(ResourceTypes.API_RESOURCE);
        assertThat(declaration.getModuleCode()).isEqualTo("gateway");
        assertThat(declaration.getTargetModule()).isEqualTo("authorization");
        assertThat(declaration.getFields().get("pathPattern").getValue()).isEqualTo("/auth/**");
    }

    private static class TestApi implements ApiResourceApi {

        @Override
        public R<ApiResourceRegisterResultVO> registerApiResources(List<ApiResourceRegisterCommand> resources) {
            return R.ok(new ApiResourceRegisterResultVO(resources.size(), resources.size(), 0));
        }

        @Override
        public R<ApiResourceAccessDecisionVO> resolveAccessDecision(ApiResourceAccessDecisionQuery query) {
            return R.ok(ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN));
        }

        @Override
        public R<Void> refreshApiResourceCache() {
            return R.ok();
        }
    }
}
