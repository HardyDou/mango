package io.mango.authorization.resource.sync;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.authorization.api.security.IPermissionProvider;
import io.mango.authorization.api.security.SecurityPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Supplier;

/**
 * 基于已注册 API 资源的 URL 级授权管理器。
 *
 * @author hardy
 */
@Slf4j
public class ApiResourceAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final ApiResourceApi apiResourceApi;
    private final IPermissionProvider permissionService;

    public ApiResourceAuthorizationManager(ApiResourceApi apiResourceApi, IPermissionProvider permissionService) {
        this.apiResourceApi = apiResourceApi;
        this.permissionService = permissionService;
    }

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        R<ApiResourceAccessDecisionVO> response = apiResourceApi.resolveAccessDecision(
                request.getMethod(),
                request.getRequestURI());
        ApiResourceAccessDecisionVO decision = response != null && response.isSuccess() && response.getData() != null
                ? response.getData()
                : ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN);
        ApiResourceAccessMode accessMode = decision.accessMode() == null
                ? ApiResourceAccessMode.LOGIN
                : decision.accessMode();
        Authentication authentication = authenticationSupplier.get();
        boolean granted = switch (accessMode) {
            case PUBLIC -> true;
            case LOGIN -> isAuthenticated(authentication);
            case PERMISSION -> hasPermission(authentication, decision.permissionCode());
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
        List<String> permissions = permissionService.listUserPermissions(principal);
        return permissions.stream().anyMatch(permission -> permissionMatches(permission, permissionCode));
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

    private boolean permissionMatches(String grantedPermission, String requiredPermission) {
        return "*:*".equals(grantedPermission) || requiredPermission.equals(grantedPermission);
    }
}
