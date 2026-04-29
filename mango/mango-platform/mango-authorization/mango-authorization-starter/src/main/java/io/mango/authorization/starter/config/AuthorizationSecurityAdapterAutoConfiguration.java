package io.mango.authorization.starter.config;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.infra.security.api.IPermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 面向 infra-security 权限提供者的授权自动配置。
 *
 * @author Mango
 */
@AutoConfiguration
@RequiredArgsConstructor
public class AuthorizationSecurityAdapterAutoConfiguration {

    /**
     * 创建权限服务实现。
     * Only registered when no other IPermissionProvider bean exists
     * (e.g., when mango-authorization-starter is not on classpath).
     */
    @Bean
    @ConditionalOnMissingBean(IPermissionProvider.class)
    public IPermissionProvider permissionService(IAuthorizationProvider authorizationProvider) {
        return userId -> authorizationProvider.load(AuthorizationQuery.user(userId))
                .permissionCodes()
                .stream()
                .toList();
    }
}
