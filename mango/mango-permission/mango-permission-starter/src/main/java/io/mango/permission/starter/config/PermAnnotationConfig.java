package io.mango.permission.starter.config;

import io.mango.common.permission.IPermissionService;
import io.mango.permission.core.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Permission auto configuration for @Perm aspect.
 *
 * @author Mango
 */
@AutoConfiguration
@RequiredArgsConstructor
public class PermAnnotationConfig {

    /**
     * Create permission service implementation
     */
    @Bean
    public IPermissionService permissionService(ISysUserService sysUserService) {
        return userId -> sysUserService.listUserPermissions(userId);
    }
}
