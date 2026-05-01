package io.mango.security.starter.remote;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.security.IPermissionProvider;
import io.mango.authorization.security.starter.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Remote security aggregate adapters.
 */
@AutoConfiguration
@AutoConfigureBefore(SecurityAutoConfiguration.class)
public class SecurityRemoteAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IPermissionProvider.class)
    public IPermissionProvider permissionService(IAuthorizationProvider authorizationProvider) {
        return userId -> authorizationProvider.load(AuthorizationQuery.user(userId))
                .permissionCodes()
                .stream()
                .toList();
    }
}
