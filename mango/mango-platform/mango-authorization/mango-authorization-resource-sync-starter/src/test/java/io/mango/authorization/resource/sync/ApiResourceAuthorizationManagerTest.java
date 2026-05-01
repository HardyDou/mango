package io.mango.authorization.resource.sync;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.authorization.api.security.IPermissionProvider;
import io.mango.authorization.api.security.SecurityPrincipal;
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

    private ApiResourceAuthorizationManager manager(
            ApiResourceAccessMode accessMode,
            String permissionCode,
            List<String> permissions) {
        ApiResourceApi api = new TestApi(accessMode, permissionCode);
        IPermissionProvider permissionService = userId -> permissions;
        return new ApiResourceAuthorizationManager(api, permissionService);
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
        public R<ApiResourceAccessDecisionVO> resolveAccessDecision(String httpMethod, String path) {
            return R.ok(new ApiResourceAccessDecisionVO(true, accessMode, permissionCode));
        }
    }
}
