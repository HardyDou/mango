package io.mango.authorization.starter.config;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.infra.security.api.IPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Authorization auto configuration for @Perm aspect.
 *
 * @author Mango
 */
@AutoConfiguration
@RequiredArgsConstructor
public class AuthorizationSecurityAdapterAutoConfiguration {

    /**
     * Create permission service implementation.
     * Only registered when no other IPermissionService bean exists
     * (e.g., when mango-authorization-starter is not on classpath).
     */
    @Bean
    @ConditionalOnMissingBean(IPermissionService.class)
    public IPermissionService permissionService(IAuthorizationProvider authorizationProvider) {
        return userId -> authorizationProvider.load(AuthorizationQuery.user(userId))
                .permissionCodes()
                .stream()
                .toList();
    }
}
