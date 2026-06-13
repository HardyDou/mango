package io.mango.authorization.resource.access;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.ApiResourceAccessDecisionQuery;
import io.mango.common.result.R;
import io.mango.authorization.api.SecurityPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("API resource authorization manager tests")
class ApiResourceAuthorizationManagerTest {

    @Test
    @DisplayName("public resource should allow anonymous access")
    void publicResourceShouldAllowAnonymousAccess() {
        ApiResourceAuthorizationManager manager = manager(ApiResourceAccessMode.PUBLIC, null, List.of());

        assertTrue(manager.check(() -> null, context("GET", "/public")).isGranted());
    }

    @Test
    @DisplayName("login resource should require authenticated request")
    void loginResourceShouldRequireAuthenticatedRequest() {
        ApiResourceAuthorizationManager manager = manager(ApiResourceAccessMode.LOGIN, null, List.of());

        assertFalse(manager.check(() -> null, context("GET", "/secure")).isGranted());
        assertTrue(manager.check(this::authentication, context("GET", "/secure")).isGranted());
    }

    @Test
    @DisplayName("permission resource should require matching permission")
    void permissionResourceShouldRequireMatchingPermission() {
        ApiResourceAuthorizationManager denied = manager(ApiResourceAccessMode.PERMISSION, "demo:read", List.of("demo:write"));
        ApiResourceAuthorizationManager granted = manager(ApiResourceAccessMode.PERMISSION, "demo:read", List.of("demo:read"));

        assertFalse(denied.check(this::authentication, context("GET", "/demo")).isGranted());
        assertTrue(granted.check(this::authentication, context("GET", "/demo")).isGranted());
    }

    @Test
    @DisplayName("permission resource should ignore request permissionCode parameter")
    void permissionResourceShouldIgnorePermissionCodeParameter() {
        ApiResourceAuthorizationManager manager = manager(ApiResourceAccessMode.PERMISSION, "demo:write", List.of("demo:read"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/demo");
        request.setParameter("permissionCode", "demo:read");

        assertFalse(manager.check(this::authentication, new RequestAuthorizationContext(request)).isGranted());
    }

    @Test
    @DisplayName("resource decision should use application path without context path")
    void resourceDecisionShouldUseApplicationPathWithoutContextPath() {
        CapturingApi api = new CapturingApi(ApiResourceAccessMode.PUBLIC);
        IAuthorizationProvider authorizationProvider =
                query -> AuthorizationSnapshot.of(List.of(), List.of(), List.of());
        ApiResourceAuthorizationManager manager = new ApiResourceAuthorizationManager(api, authorizationProvider);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/openapi/pay/orders");
        request.setContextPath("/api");
        request.setServletPath("/openapi/pay/orders");

        assertTrue(manager.check(() -> null, new RequestAuthorizationContext(request)).isGranted());
        assertEquals("/openapi/pay/orders", api.path);
    }

    @Test
    @DisplayName("external api prefix should be stripped when resource decision is unmatched")
    void externalApiPrefixShouldBeStrippedWhenResourceDecisionIsUnmatched() {
        PathDecisionApi api = new PathDecisionApi(Map.of(
                "POST /api/payment/channel-callbacks/fuiou", ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN),
                "POST /payment/channel-callbacks/fuiou", new ApiResourceAccessDecisionVO(true, ApiResourceAccessMode.PUBLIC, null)
        ));
        IAuthorizationProvider authorizationProvider =
                query -> AuthorizationSnapshot.of(List.of(), List.of(), List.of());
        ApiResourceAuthorizationManager manager = new ApiResourceAuthorizationManager(api, authorizationProvider);

        assertTrue(manager.check(() -> null, context("POST", "/api/payment/channel-callbacks/fuiou")).isGranted());
        assertEquals(2, api.resolveCount);
    }

    private ApiResourceAuthorizationManager manager(
            ApiResourceAccessMode accessMode,
            String permissionCode,
            List<String> permissions) {
        ApiResourceApi api = new TestApi(accessMode, permissionCode);
        IAuthorizationProvider authorizationProvider =
                query -> AuthorizationSnapshot.of(List.of(), permissions, permissions);
        return new ApiResourceAuthorizationManager(api, authorizationProvider);
    }

    private RequestAuthorizationContext context(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        return new RequestAuthorizationContext(request);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return UsernamePasswordAuthenticationToken.authenticated(
                new SecurityPrincipal(1L, 1001L, null, "tester", null, null, null, null, null),
                "token",
                AuthorityUtils.NO_AUTHORITIES);
    }

    private record TestApi(ApiResourceAccessMode accessMode, String permissionCode) implements ApiResourceApi {

        @Override
        public R<ApiResourceRegisterResultVO> registerApiResources(List<ApiResourceRegisterCommand> resources) {
            return R.ok(ApiResourceRegisterResultVO.empty());
        }

        @Override
        public R<ApiResourceAccessDecisionVO> resolveAccessDecision(ApiResourceAccessDecisionQuery query) {
            return R.ok(new ApiResourceAccessDecisionVO(true, accessMode, permissionCode));
        }

        @Override
        public R<Void> refreshApiResourceCache() {
            return R.ok();
        }
    }

    private static class CapturingApi implements ApiResourceApi {

        private final ApiResourceAccessMode accessMode;
        private String path;

        CapturingApi(ApiResourceAccessMode accessMode) {
            this.accessMode = accessMode;
        }

        @Override
        public R<ApiResourceRegisterResultVO> registerApiResources(List<ApiResourceRegisterCommand> resources) {
            return R.ok(ApiResourceRegisterResultVO.empty());
        }

        @Override
        public R<ApiResourceAccessDecisionVO> resolveAccessDecision(ApiResourceAccessDecisionQuery query) {
            this.path = query.getPath();
            return R.ok(new ApiResourceAccessDecisionVO(true, accessMode, null));
        }

        @Override
        public R<Void> refreshApiResourceCache() {
            return R.ok();
        }
    }

    private static class PathDecisionApi implements ApiResourceApi {

        private final Map<String, ApiResourceAccessDecisionVO> decisions;
        private int resolveCount;

        PathDecisionApi(Map<String, ApiResourceAccessDecisionVO> decisions) {
            this.decisions = decisions;
        }

        @Override
        public R<ApiResourceRegisterResultVO> registerApiResources(List<ApiResourceRegisterCommand> resources) {
            return R.ok(ApiResourceRegisterResultVO.empty());
        }

        @Override
        public R<ApiResourceAccessDecisionVO> resolveAccessDecision(ApiResourceAccessDecisionQuery query) {
            resolveCount++;
            return R.ok(decisions.getOrDefault(
                    query.getHttpMethod() + " " + query.getPath(),
                    ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN)));
        }

        @Override
        public R<Void> refreshApiResourceCache() {
            return R.ok();
        }
    }
}
