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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                new SecurityPrincipal(1L, null, "tester"),
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
}
