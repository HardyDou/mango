package io.mango.access.core.auth;

import io.mango.access.core.config.AccessProperties;
import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.ApiResourceAccessDecisionQuery;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.common.result.R;
import io.mango.authorization.api.ITokenProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 边界入口访问决策服务。
 *
 * @author Mango
 */
@Slf4j
public class AccessService {

    private final AccessProperties properties;
    private final ITokenProvider tokenService;
    private final ApiResourceApi apiResourceApi;
    private final IAuthorizationProvider authorizationProvider;
    private final List<AccessContextValidator> contextValidators;
    private final IpWhitelistMatcher ipWhitelistMatcher = new IpWhitelistMatcher();

    public AccessService(AccessProperties properties,
                         ITokenProvider tokenService,
                         ApiResourceApi apiResourceApi,
                         IAuthorizationProvider authorizationProvider) {
        this(properties, tokenService, apiResourceApi, authorizationProvider, List.of());
    }

    public AccessService(AccessProperties properties,
                         ITokenProvider tokenService,
                         ApiResourceApi apiResourceApi,
                         IAuthorizationProvider authorizationProvider,
                         List<AccessContextValidator> contextValidators) {
        this.properties = properties;
        this.tokenService = tokenService;
        this.apiResourceApi = apiResourceApi;
        this.authorizationProvider = authorizationProvider;
        this.contextValidators = contextValidators == null ? List.of() : List.copyOf(contextValidators);
    }

    public AccessResult check(String httpMethod, String path, String authHeader) {
        return check(httpMethod, path, authHeader, null);
    }

    public AccessResult check(String httpMethod, String path, String authHeader, String clientIp) {
        if (!properties.isAuthEnabled()) {
            return AccessResult.disabled();
        }
        if (ipWhitelistMatcher.matches(properties.getIpWhitelist(), httpMethod, path, clientIp)) {
            return AccessResult.allowAnonymous();
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
        AccessResult contextResult = validateContext(principal);
        if (contextResult != null) {
            return contextResult;
        }
        String requiredPermission = resolveRequiredPermission(decision.permissionCode());
        if (properties.isRequirePermissionCode()
                && accessMode == ApiResourceAccessMode.PERMISSION
                && requiredPermission == null) {
            return AccessResult.forbidden("接口未声明权限码");
        }
        if (accessMode == ApiResourceAccessMode.PERMISSION
                && !hasPermission(principal, requiredPermission)) {
            return AccessResult.forbidden("权限不足");
        }
        return AccessResult.allowAuthenticated(principal);
    }

    private AccessResult validateContext(AccessPrincipal principal) {
        for (AccessContextValidator validator : contextValidators) {
            try {
                AccessContextValidationResult result = validator.validate(principal);
                if (result != null && !result.allowed()) {
                    return AccessResult.unauthorized(result.message());
                }
            } catch (Exception e) {
                log.warn("登录上下文校验失败，拒绝本次访问: userId={}, tenantId={}, reason={}",
                        principal == null ? null : principal.userId(),
                        principal == null ? null : principal.tenantId(),
                        e.getMessage());
                return AccessResult.unauthorized("登录上下文校验失败，请重新登录");
            }
        }
        return null;
    }

    private ApiResourceAccessDecisionVO resolveDecision(String httpMethod, String path) {
        try {
            ApiResourceAccessDecisionQuery query = new ApiResourceAccessDecisionQuery();
            query.setHttpMethod(httpMethod);
            query.setPath(path);
            R<ApiResourceAccessDecisionVO> response = apiResourceApi.resolveAccessDecision(query);
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
                parseLong(tokenService.getClaim(token, "memberId")),
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

    private String resolveRequiredPermission(String resourcePermissionCode) {
        if (resourcePermissionCode != null && !resourcePermissionCode.isBlank()) {
            return resourcePermissionCode.trim();
        }
        return null;
    }

    private boolean hasPermission(AccessPrincipal principal, String permissionCode) {
        if (principal == null || principal.memberId() == null || permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        AuthorizationQuery query = AuthorizationQuery.member(principal.memberId())
                .withTenantId(principal.tenantId())
                .withSystemCode(principal.appCode())
                .withRealm(principal.realm())
                .withActorType(principal.actorType())
                .withParty(principal.partyType(), principal.partyId());
        return authorizationProvider.load(query).permissionCodes().stream()
                .anyMatch(granted -> permissionMatches(granted, permissionCode));
    }

    private boolean permissionMatches(String grantedPermission, String requiredPermission) {
        return "*:*".equals(grantedPermission) || requiredPermission.equals(grantedPermission);
    }
}
