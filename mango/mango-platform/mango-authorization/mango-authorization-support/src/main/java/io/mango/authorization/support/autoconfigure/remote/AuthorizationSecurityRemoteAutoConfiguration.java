package io.mango.authorization.support.autoconfigure.remote;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.security.IPermissionProvider;
import io.mango.authorization.support.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Remote authorization support adapters.
 */
@AutoConfiguration
@AutoConfigureBefore(SecurityAutoConfiguration.class)
public class AuthorizationSecurityRemoteAutoConfiguration {

    @Bean
    @ConditionalOnBean(IAuthorizationProvider.class)
    @ConditionalOnMissingBean(IPermissionProvider.class)
    public IPermissionProvider permissionService(IAuthorizationProvider authorizationProvider) {
        return userId -> authorizationProvider.load(AuthorizationQuery.user(userId))
                .permissionCodes()
                .stream()
                .toList();
    }
}
