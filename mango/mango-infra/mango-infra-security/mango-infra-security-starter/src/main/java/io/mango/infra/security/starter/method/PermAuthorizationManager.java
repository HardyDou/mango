package io.mango.infra.security.starter.method;

import io.mango.infra.security.api.IPermissionProvider;
import io.mango.infra.security.api.Perm;
import io.mango.infra.security.api.SecurityPrincipal;
import io.mango.infra.security.core.impl.DefaultPermissionServiceImpl;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

/**
 * 基于 {@link Perm} 的方法级权限管理器。
 */
public class PermAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private static final String SUPER_PERMISSION = "*:*";

    private final ObjectProvider<IPermissionProvider> permissionProvider;

    public PermAuthorizationManager(ObjectProvider<IPermissionProvider> permissionProvider) {
        this.permissionProvider = permissionProvider;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, MethodInvocation invocation) {
        Perm perm = findPerm(invocation);
        if (perm == null || !StringUtils.hasText(perm.value())) {
            return new AuthorizationDecision(true);
        }
        Authentication authentication = authenticationSupplier.get();
        return new AuthorizationDecision(hasPermission(authentication, perm.value()));
    }

    private Perm findPerm(MethodInvocation invocation) {
        Method method = invocation.getMethod();
        Class<?> targetClass = invocation.getThis() == null ? null : invocation.getThis().getClass();
        Method specificMethod = targetClass == null ? method : AopUtils.getMostSpecificMethod(method, targetClass);
        Perm perm = AnnotatedElementUtils.findMergedAnnotation(specificMethod, Perm.class);
        return perm != null ? perm : AnnotatedElementUtils.findMergedAnnotation(method, Perm.class);
    }

    private boolean hasPermission(Authentication authentication, String permissionCode) {
        if (!isAuthenticated(authentication)) {
            return false;
        }
        if (hasAuthority(authentication, permissionCode)) {
            return true;
        }
        Long userId = resolveUserId(authentication);
        if (userId == null) {
            return false;
        }
        IPermissionProvider provider = resolvePermissionProvider();
        if (provider == null) {
            return false;
        }
        List<String> permissions = provider.listUserPermissions(userId);
        return permissions != null && permissions.stream()
                .anyMatch(permission -> permissionMatches(permission, permissionCode));
    }

    private IPermissionProvider resolvePermissionProvider() {
        return permissionProvider.orderedStream()
                .filter(provider -> !(provider instanceof DefaultPermissionServiceImpl))
                .findFirst()
                .orElseGet(permissionProvider::getIfAvailable);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean hasAuthority(Authentication authentication, String permissionCode) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String granted = authority.getAuthority();
            if (permissionMatches(granted, permissionCode)) {
                return true;
            }
        }
        return false;
    }

    private Long resolveUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityPrincipal securityPrincipal) {
            return securityPrincipal.userId();
        }
        if (principal instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private boolean permissionMatches(String grantedPermission, String requiredPermission) {
        return SUPER_PERMISSION.equals(grantedPermission) || requiredPermission.equals(grantedPermission);
    }
}
