package io.mango.rbac.starter.config;

import io.mango.infra.security.api.IPermissionService;
import io.mango.rbac.core.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * RBAC auto configuration for @Perm aspect.
 *
 * @author Mango
 */
@AutoConfiguration
@RequiredArgsConstructor
public class RbacAnnotationConfig {

    /**
     * Create permission service implementation.
     * Only registered when no other IPermissionService bean exists
     * (e.g., when mango-rbac-starter is not on classpath).
     */
    @Bean
    @ConditionalOnMissingBean(IPermissionService.class)
    public IPermissionService permissionService(ISysUserService sysUserService) {
        return userId -> sysUserService.listUserPermissions(userId);
    }
}
