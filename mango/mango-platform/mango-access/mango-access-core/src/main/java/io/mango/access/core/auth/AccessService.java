package io.mango.access.core.auth;

import io.mango.access.core.config.AccessProperties;
import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.common.result.R;
import io.mango.authorization.api.security.IPermissionProvider;
import io.mango.authorization.api.security.ITokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 边界入口访问决策服务。
 *
 * @author Mango
 */
@Slf4j
@RequiredArgsConstructor
public class AccessService {

    private final AccessProperties properties;
    private final ITokenProvider tokenService;
    private final ApiResourceApi apiResourceApi;
    private final IPermissionProvider permissionProvider;

    public AccessResult check(String httpMethod, String path, String authHeader) {
        if (!properties.isAuthEnabled()) {
            return AccessResult.disabled();
        }

        ApiResourceAccessDecisionVO decision = resolveDecision(httpMethod, path);
        ApiResourceAccessMode accessMode = decision.accessMode() == null
                ? ApiResourceAccessMode.LOGIN
                : decision.accessMode();
        if (accessMode == ApiResourceAccessMode.INTERNAL) {
            return AccessResult.forbidden("内部接口不允许外部访问");
        }
        if (accessMode == ApiResourceAccessMode.PUBLIC) {
            return AccessResult.allowAnonymous();
        }
        if (authHeader == null || !authHeader.startsWith(ITokenProvider.BEARER_PREFIX)) {
            return AccessResult.unauthorized("缺少或非法的 Authorization 请求头");
        }

        String token = authHeader.substring(ITokenProvider.BEARER_PREFIX.length());
        if (!tokenService.validateToken(token)) {
            return AccessResult.unauthorized("Token 无效或已过期");
        }
        String tokenType = tokenService.getTokenType(token);
        if (!ITokenProvider.TOKEN_TYPE_ACCESS.equals(tokenType)) {
            return AccessResult.unauthorized("Token 类型非法，访问入口只接受 access token");
        }
        AccessPrincipal principal = resolvePrincipal(token);
        if (accessMode == ApiResourceAccessMode.PERMISSION
                && !hasPermission(principal.userId(), decision.permissionCode())) {
            return AccessResult.forbidden("权限不足");
        }
        return AccessResult.allowAuthenticated(principal);
    }

    private ApiResourceAccessDecisionVO resolveDecision(String httpMethod, String path) {
        try {
            R<ApiResourceAccessDecisionVO> response = apiResourceApi.resolveAccessDecision(httpMethod, path);
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("解析 API 访问策略失败，按登录访问处理: method={}, path={}, reason={}",
                    httpMethod, path, e.getMessage());
        }
        return ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN);
    }

    private AccessPrincipal resolvePrincipal(String token) {
        return new AccessPrincipal(
                tokenService.getUserId(token),
                tokenService.getUsername(token),
                tokenService.getClaim(token, "tenantId"),
                tokenService.getClaim(token, "realm"),
                tokenService.getClaim(token, "actorType"),
                tokenService.getClaim(token, "partyType"),
                parseLong(tokenService.getClaim(token, "partyId")),
                tokenService.getClaim(token, "appCode")
        );
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasPermission(Long userId, String permissionCode) {
        if (userId == null || permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        return permissionProvider.listUserPermissions(userId).stream()
                .anyMatch(granted -> permissionMatches(granted, permissionCode));
    }

    private boolean permissionMatches(String grantedPermission, String requiredPermission) {
        return "*:*".equals(grantedPermission) || requiredPermission.equals(grantedPermission);
    }
}
