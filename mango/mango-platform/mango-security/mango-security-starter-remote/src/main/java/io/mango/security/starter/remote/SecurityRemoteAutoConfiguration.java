package io.mango.security.starter.remote;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.infra.security.api.IPermissionService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Remote security aggregate adapters.
 */
@AutoConfiguration
public class SecurityRemoteAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IPermissionService.class)
    public IPermissionService permissionService(IAuthorizationProvider authorizationProvider) {
        return userId -> authorizationProvider.load(AuthorizationQuery.user(userId))
                .permissionCodes()
                .stream()
                .toList();
    }
}
