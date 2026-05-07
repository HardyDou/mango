package io.mango.authorization.resource.access;

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

    public ApiResourceAuthorizationManager(ApiResourceApi apiResourceApi, IAuthorizationProvider authorizationProvider) {
        this.apiResourceApi = apiResourceApi;
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        ApiResourceAccessDecisionQuery query = new ApiResourceAccessDecisionQuery();
        query.setHttpMethod(request.getMethod());
        query.setPath(request.getRequestURI());
        R<ApiResourceAccessDecisionVO> response = apiResourceApi.resolveAccessDecision(query);
        ApiResourceAccessDecisionVO decision = response != null && response.isSuccess() && response.getData() != null
                ? response.getData()
                : ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN);
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

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean hasPermission(Authentication authentication, String permissionCode) {
        if (!isAuthenticated(authentication) || !StringUtils.hasText(permissionCode)) {
            return false;
        }
        SecurityPrincipal principal = resolvePrincipal(authentication);
        if (principal == null || principal.userId() == null) {
            return false;
        }
        AuthorizationQuery query = AuthorizationQuery.user(principal.userId())
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
