package io.mango.authorization.resource.sync.gateway;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.common.result.R;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GatewayRouteResourceSyncRunnerTest {

    @Test
    @DisplayName("scanRoutes should register gateway path predicates")
    void scanRoutes_shouldRegisterGatewayPathPredicates() {
        RouteDefinition route = new RouteDefinition();
        route.setId("platform");
        route.setUri(URI.create("http://localhost:8081"));
        route.setPredicates(List.of(new PredicateDefinition("Path=/auth/**,/system/**")));

        GatewayRouteResourceSyncRunner runner = new GatewayRouteResourceSyncRunner(
                () -> Flux.just(route),
                new TestApi());

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
    }

    private static class TestApi implements ApiResourceApi {

        @Override
        public R<ApiResourceRegisterResultVO> registerApiResources(List<ApiResourceRegisterCommand> resources) {
            return R.ok(new ApiResourceRegisterResultVO(resources.size(), resources.size(), 0));
        }

        @Override
        public R<ApiResourceAccessDecisionVO> resolveAccessDecision(String httpMethod, String path) {
            return R.ok(ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN));
        }
    }
}
