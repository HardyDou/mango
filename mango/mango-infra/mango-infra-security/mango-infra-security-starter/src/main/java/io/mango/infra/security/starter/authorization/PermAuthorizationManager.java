package io.mango.infra.security.starter.authorization;

import io.mango.infra.security.api.IPermissionService;
import io.mango.infra.security.api.Perm;
import io.mango.infra.security.api.SecurityPrincipal;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Spring Security authorization manager for {@link Perm}.
 */
public class PermAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private static final String SUPER_ADMIN_PERMISSION = "*:*";

    private final IPermissionService permissionService;

    public PermAuthorizationManager(IPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, MethodInvocation invocation) {
        Perm perm = findPerm(invocation);
        if (perm == null) {
            return new AuthorizationDecision(true);
        }

        Authentication authentication = authenticationSupplier.get();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        String requiredPermission = perm.value();
        if (hasAuthority(authentication, requiredPermission)) {
            return new AuthorizationDecision(true);
        }

        Long userId = resolveUserId(authentication);
        if (userId == null) {
            return new AuthorizationDecision(false);
        }

        boolean granted = permissionService.listUserPermissions(userId).stream()
                .anyMatch(permission -> permissionMatches(permission, requiredPermission));
        return new AuthorizationDecision(granted);
    }

    private Perm findPerm(MethodInvocation invocation) {
        Method method = invocation.getMethod();
        Class<?> targetClass = invocation.getThis() == null ? method.getDeclaringClass() : AopUtils.getTargetClass(invocation.getThis());
        Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        return AnnotatedElementUtils.findMergedAnnotation(targetMethod, Perm.class);
    }

    private boolean hasAuthority(Authentication authentication, String requiredPermission) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> permissionMatches(authority, requiredPermission));
    }

    private boolean permissionMatches(String grantedPermission, String requiredPermission) {
        return SUPER_ADMIN_PERMISSION.equals(grantedPermission) || requiredPermission.equals(grantedPermission);
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
}
