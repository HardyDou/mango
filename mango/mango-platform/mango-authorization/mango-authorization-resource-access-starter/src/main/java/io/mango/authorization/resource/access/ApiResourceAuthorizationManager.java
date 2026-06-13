package io.mango.authorization.resource.access;

import io.mango.access.core.config.AccessProperties;
import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.ApiResourceAccessDecisionQuery;
import io.mango.common.result.R;
import io.mango.authorization.api.SecurityPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * 基于已注册 API 资源的 URL 级授权管理器。
 *
 * @author hardy
 */
public class ApiResourceAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final ApiResourceApi apiResourceApi;
    private final IAuthorizationProvider authorizationProvider;
    private final AccessProperties accessProperties;

    public ApiResourceAuthorizationManager(ApiResourceApi apiResourceApi, IAuthorizationProvider authorizationProvider) {
        this(apiResourceApi, authorizationProvider, new AccessProperties());
    }

    public ApiResourceAuthorizationManager(ApiResourceApi apiResourceApi,
                                           IAuthorizationProvider authorizationProvider,
                                           AccessProperties accessProperties) {
        this.apiResourceApi = apiResourceApi;
        this.authorizationProvider = authorizationProvider;
        this.accessProperties = accessProperties == null ? new AccessProperties() : accessProperties;
    }

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        ApiResourceAccessDecisionVO decision = resolveAccessDecision(request.getMethod(), resolveApplicationPath(request));
        ApiResourceAccessMode accessMode = decision.accessMode() == null
                ? ApiResourceAccessMode.LOGIN
                : decision.accessMode();
        boolean granted = switch (accessMode) {
            case PUBLIC -> true;
            case LOGIN -> isAuthenticated(authenticationSupplier.get());
            case PERMISSION -> hasPermission(authenticationSupplier.get(), resolveRequiredPermission(decision.permissionCode()));
            case INTERNAL -> false;
        };
        return new AuthorizationDecision(granted);
    }

    private ApiResourceAccessDecisionVO resolveAccessDecision(String httpMethod, String path) {
        ApiResourceAccessDecisionVO decision = resolveAccessDecisionOnce(httpMethod, path);
        if (decision.matched()) {
            return decision;
        }
        String applicationPath = stripExternalApiPrefix(path);
        if (applicationPath == null) {
            return decision;
        }
        ApiResourceAccessDecisionVO applicationDecision = resolveAccessDecisionOnce(httpMethod, applicationPath);
        return applicationDecision.matched() ? applicationDecision : decision;
    }

    private ApiResourceAccessDecisionVO resolveAccessDecisionOnce(String httpMethod, String path) {
        ApiResourceAccessDecisionQuery query = new ApiResourceAccessDecisionQuery();
        query.setHttpMethod(httpMethod);
        query.setPath(path);
        R<ApiResourceAccessDecisionVO> response = apiResourceApi.resolveAccessDecision(query);
        return response != null && response.isSuccess() && response.getData() != null
                ? response.getData()
                : ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String resolveApplicationPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        String path = firstText(servletPath);
        if (StringUtils.hasText(pathInfo)) {
            path = (path == null ? "" : path) + pathInfo;
        }
        if (StringUtils.hasText(path)) {
            return path;
        }
        String requestUri = firstText(request.getRequestURI());
        String contextPath = firstText(request.getContextPath());
        if (requestUri != null && contextPath != null && requestUri.startsWith(contextPath)) {
            String strippedPath = requestUri.substring(contextPath.length());
            return StringUtils.hasText(strippedPath) ? strippedPath : "/";
        }
        return requestUri == null ? "/" : requestUri;
    }

    private String firstText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String stripExternalApiPrefix(String path) {
        if (!StringUtils.hasText(path) || accessProperties.getExternalApiPrefixes() == null) {
            return null;
        }
        for (String prefix : accessProperties.getExternalApiPrefixes()) {
            String normalizedPrefix = normalizePrefix(prefix);
            if (normalizedPrefix == null || !path.startsWith(normalizedPrefix + "/")) {
                continue;
            }
            String stripped = path.substring(normalizedPrefix.length());
            return StringUtils.hasText(stripped) ? stripped : null;
        }
        return null;
    }

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
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

    private boolean hasPermission(Authentication authentication, String permissionCode) {
        if (!isAuthenticated(authentication) || !StringUtils.hasText(permissionCode)) {
            return false;
        }
        SecurityPrincipal principal = resolvePrincipal(authentication);
        if (principal == null || principal.memberId() == null) {
            return false;
        }
        AuthorizationQuery query = AuthorizationQuery.member(principal.memberId())
                .withTenantId(principal.tenantId())
                .withSystemCode(principal.appCode())
                .withRealm(principal.realm())
                .withActorType(principal.actorType())
                .withParty(principal.partyType(), principal.partyId());
        return authorizationProvider.load(query).permissionCodes().stream()
                .anyMatch(permission -> permissionMatches(permission, permissionCode));
    }

    private SecurityPrincipal resolvePrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityPrincipal securityPrincipal) {
            return securityPrincipal;
        }
        if (principal instanceof Number number) {
            return new SecurityPrincipal(number.longValue(), null, null);
        }
        return null;
    }

    private String resolveRequiredPermission(String resourcePermissionCode) {
        if (StringUtils.hasText(resourcePermissionCode)) {
            return resourcePermissionCode.trim();
        }
        return null;
    }

    private boolean permissionMatches(String grantedPermission, String requiredPermission) {
        return "*:*".equals(grantedPermission) || requiredPermission.equals(grantedPermission);
    }
}
