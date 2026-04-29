package io.mango.infra.security.starter.context;

import io.mango.infra.security.api.ISecurityContextProvider;
import io.mango.infra.security.api.SecurityContext;
import io.mango.infra.security.api.SecurityPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 基于 Spring Security 的安全上下文提供器。
 */
public class SpringSecurityContextProvider implements ISecurityContextProvider {

    @Override
    public SecurityContext currentContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SecurityContext.anonymous();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityPrincipal securityPrincipal) {
            return new SecurityContext(
                    securityPrincipal.userId(),
                    securityPrincipal.tenantId(),
                    true,
                    securityPrincipal.principalName(),
                    securityPrincipal.realm(),
                    securityPrincipal.actorType(),
                    securityPrincipal.partyType(),
                    securityPrincipal.partyId(),
                    securityPrincipal.appCode());
        }

        String principalName = authentication.getName();
        Long userId = principal instanceof Number number ? number.longValue() : null;
        return new SecurityContext(userId, null, true, principalName);
    }
}
