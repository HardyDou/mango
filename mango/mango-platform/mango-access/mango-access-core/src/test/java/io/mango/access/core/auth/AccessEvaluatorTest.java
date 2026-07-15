package io.mango.access.core.auth;

import io.mango.access.api.vo.AccessContextValidationResultVO;
import io.mango.access.api.vo.AccessResultVO;
import io.mango.access.core.config.AccessProperties;
import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.api.command.ApiResourceRegisterRequest;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.ApiResourceAccessDecisionQuery;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.authorization.api.vo.TokenPairVO;
import io.mango.common.result.R;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AccessEvaluatorTest {

    private final ResourceApi resourceApi = new ResourceApi();
    private final TokenProvider tokenProvider = new TokenProvider();
    private final AuthorizationProvider authorizationProvider = new AuthorizationProvider();
    private final AccessProperties properties = new AccessProperties();

    @Test
    @DisplayName("PUBLIC 与 INTERNAL 应分别匿名放行和拒绝")
    void check_shouldApplyPublicAndInternalModes() {
        resourceApi.mode = ApiResourceAccessMode.PUBLIC;
        assertEquals(AccessResultVO.Status.ALLOW_ANONYMOUS, evaluator().check("GET", "/public", null, null).status());

        resourceApi.mode = ApiResourceAccessMode.INTERNAL;
        AccessResultVO internal = evaluator().check("GET", "/internal", "Bearer valid", null);
        assertEquals(AccessResultVO.Status.FORBIDDEN, internal.status());
        assertEquals("内部接口不允许外部访问", internal.message());
    }

    @Test
    @DisplayName("策略响应失败、空响应和异常均应关闭访问")
    void check_shouldFailClosedWhenPolicyDependencyUnavailable() {
        resourceApi.response = R.fail("offline");
        assertUnavailable(evaluator().check("GET", "/users", null, null), "访问策略服务暂不可用");

        resourceApi.response = null;
        resourceApi.returnNull = true;
        assertUnavailable(evaluator().check("GET", "/users", null, null), "访问策略服务暂不可用");

        resourceApi.returnNull = false;
        resourceApi.throwFailure = true;
        assertUnavailable(evaluator().check("GET", "/users", null, null), "访问策略服务暂不可用");
    }

    @Test
    @DisplayName("合法 access token 应解析完整主体")
    void check_shouldResolveAuthenticatedPrincipal() {
        resourceApi.mode = ApiResourceAccessMode.LOGIN;

        AccessResultVO result = evaluator().check("GET", "/profile", "Bearer valid", null);

        assertEquals(AccessResultVO.Status.ALLOW_AUTHENTICATED, result.status());
        assertEquals(101L, result.principal().userId());
        assertEquals(201L, result.principal().memberId());
        assertEquals("tenant-a", result.principal().tenantId());
        assertEquals("app-a", result.principal().appCode());
    }

    @Test
    @DisplayName("缺失、无效与 refresh token 应返回 401")
    void check_shouldRejectInvalidCredentials() {
        assertEquals(AccessResultVO.Status.UNAUTHORIZED, evaluator().check("GET", "/profile", null, null).status());
        assertEquals(AccessResultVO.Status.UNAUTHORIZED,
                evaluator().check("GET", "/profile", "Bearer invalid", null).status());
        tokenProvider.tokenType = ITokenProvider.TOKEN_TYPE_REFRESH;
        assertEquals(AccessResultVO.Status.UNAUTHORIZED,
                evaluator().check("GET", "/profile", "Bearer valid", null).status());
    }

    @Test
    @DisplayName("权限模式应按注册权限精确匹配并支持超级权限")
    void check_shouldApplyRegisteredPermission() {
        resourceApi.mode = ApiResourceAccessMode.PERMISSION;
        resourceApi.permissionCode = "system:user:create";
        authorizationProvider.permissions = List.of("system:user:view");
        assertEquals(AccessResultVO.Status.FORBIDDEN,
                evaluator().check("POST", "/users", "Bearer valid", null).status());

        authorizationProvider.permissions = List.of("system:user:create");
        assertEquals(AccessResultVO.Status.ALLOW_AUTHENTICATED,
                evaluator().check("POST", "/users", "Bearer valid", null).status());

        authorizationProvider.permissions = List.of("*:*");
        assertEquals(AccessResultVO.Status.ALLOW_AUTHENTICATED,
                evaluator().check("POST", "/users", "Bearer valid", null).status());
    }

    @Test
    @DisplayName("权限提供者空响应或异常均应关闭访问")
    void check_shouldFailClosedWhenAuthorizationUnavailable() {
        resourceApi.mode = ApiResourceAccessMode.PERMISSION;
        resourceApi.permissionCode = "system:user:create";
        authorizationProvider.returnNull = true;
        assertUnavailable(evaluator().check("POST", "/users", "Bearer valid", null), "权限服务暂不可用");

        authorizationProvider.returnNull = false;
        authorizationProvider.throwFailure = true;
        assertUnavailable(evaluator().check("POST", "/users", "Bearer valid", null), "权限服务暂不可用");
    }

    @Test
    @DisplayName("上下文校验拒绝与异常应稳定返回 401")
    void check_shouldApplyContextValidators() {
        AccessEvaluator denied = new AccessEvaluator(properties, tokenProvider, resourceApi, authorizationProvider,
                List.of(principal -> AccessContextValidationResultVO.deny("机构已停用")));
        assertEquals("机构已停用", denied.check("GET", "/profile", "Bearer valid", null).message());

        AccessEvaluator failed = new AccessEvaluator(properties, tokenProvider, resourceApi, authorizationProvider,
                List.of(principal -> { throw new IllegalStateException("offline"); }));
        AccessResultVO result = failed.check("GET", "/profile", "Bearer valid", null);
        assertEquals(AccessResultVO.Status.UNAUTHORIZED, result.status());
        assertEquals("登录上下文校验失败，请重新登录", result.message());
    }

    @Test
    @DisplayName("关闭认证或命中 IP 白名单时不应调用策略依赖")
    void check_shouldBypassOnlyForExplicitConfiguration() {
        properties.setAuthEnabled(false);
        assertEquals(AccessResultVO.Status.AUTH_DISABLED, evaluator().check("GET", "/any", null, null).status());
        assertEquals(0, resourceApi.calls);

        properties.setAuthEnabled(true);
        properties.getIpWhitelist().setEnabled(true);
        AccessProperties.Rule rule = new AccessProperties.Rule();
        rule.setPathPattern("/actuator/**");
        rule.setMethods(List.of("GET"));
        rule.setCidrs(List.of("10.0.0.0/8"));
        properties.getIpWhitelist().setRules(List.of(rule));
        assertEquals(AccessResultVO.Status.ALLOW_ANONYMOUS,
                evaluator().check("GET", "/actuator/health", null, "10.1.2.3").status());
        assertEquals(0, resourceApi.calls);
    }

    private AccessEvaluator evaluator() {
        return new AccessEvaluator(properties, tokenProvider, resourceApi, authorizationProvider);
    }

    private void assertUnavailable(AccessResultVO result, String message) {
        assertEquals(AccessResultVO.Status.SERVICE_UNAVAILABLE, result.status());
        assertEquals(message, result.message());
        assertNull(result.principal());
    }

    private static final class ResourceApi implements ApiResourceApi {
        private ApiResourceAccessMode mode = ApiResourceAccessMode.LOGIN;
        private String permissionCode;
        private R<ApiResourceAccessDecisionVO> response;
        private boolean returnNull;
        private boolean throwFailure;
        private int calls;

        @Override public R<ApiResourceRegisterResultVO> registerApiResources(ApiResourceRegisterRequest request) {
            return R.ok(ApiResourceRegisterResultVO.empty());
        }

        @Override public R<ApiResourceAccessDecisionVO> resolveAccessDecision(ApiResourceAccessDecisionQuery query) {
            calls++;
            if (throwFailure) throw new IllegalStateException("offline");
            if (returnNull) return null;
            if (response != null) return response;
            return R.ok(new ApiResourceAccessDecisionVO(true, mode, permissionCode));
        }

        @Override public R<Void> refreshApiResourceCache() { return R.ok(); }
    }

    private static final class TokenProvider implements ITokenProvider {
        private String tokenType = TOKEN_TYPE_ACCESS;
        private final Map<String, String> claims = Map.of(
                "memberId", "201", "tenantId", "tenant-a", "realm", "INTERNAL",
                "actorType", "USER", "partyType", "COMPANY", "partyId", "301", "appCode", "app-a");

        @Override public String generateAccessToken(Long userId, String username, Map<String, Object> claims) { return "valid"; }
        @Override public String generateRefreshToken(Long userId, String username) { return "refresh"; }
        @Override public boolean validateToken(String token) { return "valid".equals(token); }
        @Override public Long getUserId(String token) { return 101L; }
        @Override public String getUsername(String token) { return "admin"; }
        @Override public String getTokenType(String token) { return tokenType; }
        @Override public String getClaim(String token, String name) { return claims.get(name); }
        @Override public TokenPairVO refresh(String refreshToken) { return new TokenPairVO("valid", "refresh"); }
    }

    private static final class AuthorizationProvider implements IAuthorizationProvider {
        private List<String> permissions = List.of("*:*");
        private boolean returnNull;
        private boolean throwFailure;

        @Override public AuthorizationSnapshotVO load(AuthorizationQuery query) {
            if (throwFailure) throw new IllegalStateException("offline");
            return returnNull ? null : AuthorizationSnapshotVO.of(List.of(), permissions, permissions);
        }
    }
}
