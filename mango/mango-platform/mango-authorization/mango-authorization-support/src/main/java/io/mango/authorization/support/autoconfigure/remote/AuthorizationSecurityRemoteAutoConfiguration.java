package io.mango.authorization.support.autoconfigure.remote;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.security.IPermissionProvider;
import io.mango.authorization.api.security.SecurityPrincipal;
import io.mango.authorization.support.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

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
        return new IPermissionProvider() {
            @Override
            public List<String> listUserPermissions(Long userId) {
                if (userId == null) {
                    return List.of();
                }
                return loadPermissions(authorizationProvider, AuthorizationQuery.user(userId));
            }

            @Override
            public List<String> listUserPermissions(SecurityPrincipal principal) {
                if (principal == null || principal.userId() == null) {
                    return List.of();
                }
                AuthorizationQuery query = AuthorizationQuery.user(principal.userId())
                        .withTenantId(principal.tenantId())
                        .withSystemCode(principal.appCode())
                        .withRealm(principal.realm())
                        .withActorType(principal.actorType())
                        .withParty(principal.partyType(), principal.partyId());
                return loadPermissions(authorizationProvider, query);
            }
        };
    }

    private List<String> loadPermissions(
            IAuthorizationProvider authorizationProvider,
            AuthorizationQuery query) {
        return authorizationProvider.load(query)
                .permissionCodes()
                .stream()
                .toList();
    }
}
