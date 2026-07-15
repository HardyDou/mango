package io.mango.access.core.auth;

import io.mango.access.api.auth.AccessContextValidator;
import io.mango.access.api.vo.AccessContextValidationResultVO;
import io.mango.access.api.vo.AccessPrincipalVO;
import io.mango.access.api.vo.AccessResultVO;
import io.mango.access.core.config.AccessProperties;
import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.ApiResourceAccessDecisionQuery;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.common.result.R;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/** 边界入口访问决策器。 */
@Slf4j
public class AccessEvaluator {

    private static final String POLICY_UNAVAILABLE = "访问策略服务暂不可用";
    private static final String AUTHORIZATION_UNAVAILABLE = "权限服务暂不可用";

    private final AccessProperties properties;
    private final ITokenProvider tokenProvider;
    private final ApiResourceApi apiResourceApi;
    private final IAuthorizationProvider authorizationProvider;
    private final List<AccessContextValidator> contextValidators;
    private final IpWhitelistMatcher ipWhitelistMatcher = new IpWhitelistMatcher();

    public AccessEvaluator(AccessProperties properties,
                           ITokenProvider tokenProvider,
                           ApiResourceApi apiResourceApi,
                           IAuthorizationProvider authorizationProvider) {
        this(properties, tokenProvider, apiResourceApi, authorizationProvider, List.of());
    }

    public AccessEvaluator(AccessProperties properties,
                           ITokenProvider tokenProvider,
                           ApiResourceApi apiResourceApi,
                           IAuthorizationProvider authorizationProvider,
                           List<AccessContextValidator> contextValidators) {
        this.properties = properties;
        this.tokenProvider = tokenProvider;
        this.apiResourceApi = apiResourceApi;
        this.authorizationProvider = authorizationProvider;
        this.contextValidators = contextValidators == null ? List.of() : List.copyOf(contextValidators);
    }

    public AccessResultVO check(String httpMethod, String path, String credential, String clientIp) {
        if (!properties.isAuthEnabled()) {
            return AccessResultVO.disabled();
        }
        if (ipWhitelistMatcher.matches(properties.getIpWhitelist(), httpMethod, path, clientIp)) {
            return AccessResultVO.allowAnonymous();
        }

        DecisionLookup lookup = resolveDecision(httpMethod, path);
        if (!lookup.available) {
            return AccessResultVO.unavailable(POLICY_UNAVAILABLE);
        }
        ApiResourceAccessDecisionVO decision = lookup.decision;
        ApiResourceAccessMode accessMode = decision.accessMode() == null
                ? ApiResourceAccessMode.LOGIN : decision.accessMode();
        if (accessMode == ApiResourceAccessMode.INTERNAL) {
            return AccessResultVO.forbidden("内部接口不允许外部访问");
        }
        if (accessMode == ApiResourceAccessMode.PUBLIC) {
            return AccessResultVO.allowAnonymous();
        }
        if (credential == null || !credential.startsWith(ITokenProvider.BEARER_PREFIX)) {
            return AccessResultVO.unauthorized("缺少或非法的 Authorization 请求头");
        }

        try {
            String token = credential.substring(ITokenProvider.BEARER_PREFIX.length());
            if (!tokenProvider.validateToken(token)) {
                return AccessResultVO.unauthorized("Token 无效或已过期");
            }
            if (!ITokenProvider.TOKEN_TYPE_ACCESS.equals(tokenProvider.getTokenType(token))) {
                return AccessResultVO.unauthorized("Token 类型非法，访问入口只接受 access token");
            }
            AccessPrincipalVO principal = resolvePrincipal(token);
            AccessResultVO contextResult = validateContext(principal);
            if (contextResult != null) {
                return contextResult;
            }
            String permissionCode = normalizePermission(decision.permissionCode());
            if (properties.isRequirePermissionCode()
                    && accessMode == ApiResourceAccessMode.PERMISSION
                    && permissionCode == null) {
                return AccessResultVO.forbidden("接口未声明权限码");
            }
            if (accessMode == ApiResourceAccessMode.PERMISSION) {
                PermissionLookup permissionLookup = hasPermission(principal, permissionCode);
                if (!permissionLookup.available) {
                    return AccessResultVO.unavailable(AUTHORIZATION_UNAVAILABLE);
                }
                if (!permissionLookup.granted) {
                    return AccessResultVO.forbidden("权限不足");
                }
            }
            return AccessResultVO.allowAuthenticated(principal);
        } catch (RuntimeException exception) {
            log.warn("访问凭证服务调用失败，拒绝本次访问: method={}, path={}, reason={}",
                    httpMethod, path, exception.getMessage());
            return AccessResultVO.unavailable("认证服务暂不可用");
        }
    }

    private AccessResultVO validateContext(AccessPrincipalVO principal) {
        for (AccessContextValidator validator : contextValidators) {
            try {
                AccessContextValidationResultVO result = validator.validate(principal);
                if (result != null && !result.allowed()) {
                    return AccessResultVO.unauthorized(result.message());
                }
            } catch (RuntimeException exception) {
                log.warn("登录上下文校验失败，拒绝本次访问: userId={}, tenantId={}, reason={}",
                        principal == null ? null : principal.userId(),
                        principal == null ? null : principal.tenantId(), exception.getMessage());
                return AccessResultVO.unauthorized("登录上下文校验失败，请重新登录");
            }
        }
        return null;
    }

    private DecisionLookup resolveDecision(String httpMethod, String path) {
        DecisionLookup first = resolveDecisionOnce(httpMethod, path);
        if (!first.available || isMatched(first.decision)) {
            return first;
        }
        String applicationPath = stripExternalApiPrefix(path);
        if (applicationPath != null) {
            DecisionLookup fallback = resolveDecisionOnce(httpMethod, applicationPath);
            if (!fallback.available || isMatched(fallback.decision)) {
                return fallback;
            }
        }
        return DecisionLookup.available(ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN));
    }

    private DecisionLookup resolveDecisionOnce(String httpMethod, String path) {
        try {
            ApiResourceAccessDecisionQuery query = new ApiResourceAccessDecisionQuery();
            query.setHttpMethod(httpMethod);
            query.setPath(path);
            R<ApiResourceAccessDecisionVO> response = apiResourceApi.resolveAccessDecision(query);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                log.warn("解析 API 访问策略失败: method={}, path={}", httpMethod, path);
                return DecisionLookup.unavailable();
            }
            return DecisionLookup.available(response.getData());
        } catch (RuntimeException exception) {
            log.warn("解析 API 访问策略异常: method={}, path={}, reason={}",
                    httpMethod, path, exception.getMessage());
            return DecisionLookup.unavailable();
        }
    }

    private boolean isMatched(ApiResourceAccessDecisionVO decision) {
        return decision != null && decision.matched();
    }

    private String stripExternalApiPrefix(String path) {
        if (path == null || path.isBlank() || properties.getExternalApiPrefixes() == null) {
            return null;
        }
        for (String prefix : properties.getExternalApiPrefixes()) {
            String normalized = normalizePrefix(prefix);
            if (normalized != null && path.startsWith(normalized + "/")) {
                return path.substring(normalized.length());
            }
        }
        return null;
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return null;
        }
        String normalized = prefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return "/".equals(normalized) ? null : normalized;
    }

    private AccessPrincipalVO resolvePrincipal(String token) {
        return new AccessPrincipalVO(
                tokenProvider.getUserId(token),
                parseLong(tokenProvider.getClaim(token, "memberId")),
                tokenProvider.getUsername(token),
                tokenProvider.getClaim(token, "tenantId"),
                tokenProvider.getClaim(token, "realm"),
                tokenProvider.getClaim(token, "actorType"),
                tokenProvider.getClaim(token, "partyType"),
                parseLong(tokenProvider.getClaim(token, "partyId")),
                tokenProvider.getClaim(token, "appCode"));
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalizePermission(String permissionCode) {
        return permissionCode == null || permissionCode.isBlank() ? null : permissionCode.trim();
    }

    private PermissionLookup hasPermission(AccessPrincipalVO principal, String permissionCode) {
        if (principal == null || principal.memberId() == null || permissionCode == null) {
            return PermissionLookup.denied();
        }
        try {
            AuthorizationQuery query = AuthorizationQuery.member(principal.memberId())
                    .withTenantId(principal.tenantId())
                    .withSystemCode(principal.appCode())
                    .withRealm(principal.realm())
                    .withActorType(principal.actorType())
                    .withParty(principal.partyType(), principal.partyId());
            AuthorizationSnapshotVO snapshot = authorizationProvider.load(query);
            if (snapshot == null || snapshot.permissionCodes() == null) {
                return PermissionLookup.unavailable();
            }
            boolean granted = snapshot.permissionCodes().stream()
                    .anyMatch(value -> "*:*".equals(value) || permissionCode.equals(value));
            return granted ? PermissionLookup.granted() : PermissionLookup.denied();
        } catch (RuntimeException exception) {
            log.warn("加载授权快照失败: memberId={}, permissionCode={}, reason={}",
                    principal.memberId(), permissionCode, exception.getMessage());
            return PermissionLookup.unavailable();
        }
    }

    private static final class DecisionLookup {
        private final boolean available;
        private final ApiResourceAccessDecisionVO decision;

        private DecisionLookup(boolean available, ApiResourceAccessDecisionVO decision) {
            this.available = available;
            this.decision = decision;
        }

        private static DecisionLookup available(ApiResourceAccessDecisionVO decision) {
            return new DecisionLookup(true, decision);
        }

        private static DecisionLookup unavailable() {
            return new DecisionLookup(false, null);
        }
    }

    private static final class PermissionLookup {
        private final boolean available;
        private final boolean granted;

        private PermissionLookup(boolean available, boolean granted) {
            this.available = available;
            this.granted = granted;
        }

        private static PermissionLookup granted() { return new PermissionLookup(true, true); }
        private static PermissionLookup denied() { return new PermissionLookup(true, false); }
        private static PermissionLookup unavailable() { return new PermissionLookup(false, false); }
    }
}
