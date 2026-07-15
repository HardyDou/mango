package io.mango.authorization.support.autoconfigure.context;

import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.api.vo.SecurityContextVO;
import io.mango.authorization.api.vo.SecurityPrincipalVO;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 基于 Spring Security 的安全上下文提供器。
 */
public class SpringSecurityContextProvider implements ISecurityContextProvider {

    @Override
    public SecurityContextVO currentContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SecurityContextVO.anonymous();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityPrincipalVO securityPrincipal) {
            return new SecurityContextVO(
                    securityPrincipal.userId(),
                    securityPrincipal.memberId(),
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
        return new SecurityContextVO(userId, null, true, principalName);
    }
}
