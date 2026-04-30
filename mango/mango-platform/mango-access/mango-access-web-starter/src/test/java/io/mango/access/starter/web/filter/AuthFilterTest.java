package io.mango.access.starter.web.filter;

import io.mango.access.core.auth.AccessService;
import io.mango.access.core.config.AccessProperties;
import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.common.result.R;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.security.api.IPermissionProvider;
import io.mango.infra.security.api.ITokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthFilterTest {

    private final TestApiResourceApi apiResourceApi = new TestApiResourceApi();
    private final TestTokenProvider tokenProvider = new TestTokenProvider();
    private final TestPermissionProvider permissionProvider = new TestPermissionProvider();

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("PUBLIC 资源应匿名放行且不写入用户上下文")
    void doFilter_shouldPassAnonymousWhenPublic() throws Exception {
        apiResourceApi.accessMode = ApiResourceAccessMode.PUBLIC;
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/public/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        newFilter().doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNull(request.getAttribute("userId"));
        assertNull(MangoContextHolder.userId());
    }

    @Test
    @DisplayName("LOGIN 资源缺少 Token 时应返回 401")
    void doFilter_shouldRejectLoginWhenTokenMissing() throws Exception {
        apiResourceApi.accessMode = ApiResourceAccessMode.LOGIN;
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        newFilter().doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertEquals("{\"code\":401,\"message\":\"缺少或非法的 Authorization 请求头\"}",
                response.getContentAsString());
    }

    @Test
    @DisplayName("LOGIN 资源 Token 合法时应放行并写入请求与 MangoContext")
    void doFilter_shouldPassLoginAndWriteContextWhenTokenValid() throws Exception {
        apiResourceApi.accessMode = ApiResourceAccessMode.LOGIN;
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        newFilter().doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertEquals(1001L, request.getAttribute("userId"));
        assertEquals("admin", request.getAttribute("username"));
        assertEquals("tenant-a", request.getAttribute("tenantId"));
        assertEquals(1001L, MangoContextHolder.userId());
        assertEquals("admin", MangoContextHolder.principalName());
        assertEquals("tenant-a", MangoContextHolder.tenantId());
        assertEquals("internal-admin", MangoContextHolder.appCode());
    }

    @Test
    @DisplayName("PERMISSION 资源权限不足时应返回 403")
    void doFilter_shouldRejectPermissionWhenPermissionMissing() throws Exception {
        apiResourceApi.accessMode = ApiResourceAccessMode.PERMISSION;
        apiResourceApi.permissionCode = "system:user:create";
        permissionProvider.permissions = List.of("system:user:view");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        newFilter().doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
        assertEquals("{\"code\":403,\"message\":\"权限不足\"}", response.getContentAsString());
    }

    private AuthFilter newFilter() {
        AccessProperties properties = new AccessProperties();
        AccessService accessService = new AccessService(properties, tokenProvider, apiResourceApi, permissionProvider);
        return new AuthFilter(accessService);
    }

    private static class TestApiResourceApi implements ApiResourceApi {

        private ApiResourceAccessMode accessMode = ApiResourceAccessMode.LOGIN;
        private String permissionCode;

        @Override
        public R<ApiResourceRegisterResultVO> registerApiResources(List<ApiResourceRegisterCommand> resources) {
            return R.ok(ApiResourceRegisterResultVO.empty());
        }

        @Override
        public R<ApiResourceAccessDecisionVO> resolveAccessDecision(String httpMethod, String path) {
            return R.ok(new ApiResourceAccessDecisionVO(true, accessMode, permissionCode));
        }
    }

    private static class TestTokenProvider implements ITokenProvider {

        private final Map<String, String> claims = Map.of(
                "tenantId", "tenant-a",
                "realm", "INTERNAL",
                "actorType", "INTERNAL_USER",
                "partyType", "COMPANY",
                "partyId", "9001",
                "appCode", "internal-admin"
        );

        @Override
        public String generateAccessToken(Long userId, String username, Map<String, Object> extraClaims) {
            return "valid-token";
        }

        @Override
        public String generateRefreshToken(Long userId, String username) {
            return "refresh-token";
        }

        @Override
        public boolean validateToken(String token) {
            return "valid-token".equals(token);
        }

        @Override
        public Long getUserId(String token) {
            return 1001L;
        }

        @Override
        public String getUsername(String token) {
            return "admin";
        }

        @Override
        public String getTokenType(String token) {
            return TOKEN_TYPE_ACCESS;
        }

        @Override
        public String getClaim(String token, String claimName) {
            return claims.get(claimName);
        }

        @Override
        public TokenPair refresh(String refreshToken) {
            return new TokenPair("valid-token", "refresh-token");
        }
    }

    private static class TestPermissionProvider implements IPermissionProvider {

        private List<String> permissions = List.of("*:*");

        @Override
        public List<String> listUserPermissions(Long userId) {
            return permissions;
        }
    }
}
