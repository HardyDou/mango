package io.mango.permission.starter.config;

import io.mango.infra.security.api.IPermissionService;
import io.mango.permission.core.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Permission auto configuration for @Perm aspect.
 *
 * @author Mango
 */
@AutoConfiguration
@RequiredArgsConstructor
public class PermAnnotationConfig {

    /**
     * Create permission service implementation.
     * Only registered when no other IPermissionService bean exists
     * (e.g., when mango-permission-starter is not on classpath).
     */
    @Bean
    @ConditionalOnMissingBean(IPermissionService.class)
    public IPermissionService permissionService(ISysUserService sysUserService) {
        return userId -> sysUserService.listUserPermissions(userId);
    }
}
