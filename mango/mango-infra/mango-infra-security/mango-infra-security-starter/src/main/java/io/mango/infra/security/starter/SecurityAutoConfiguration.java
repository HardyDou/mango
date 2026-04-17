package io.mango.infra.security.starter;

import io.mango.infra.security.api.IPermissionService;
import io.mango.infra.security.core.impl.DefaultPermissionServiceImpl;
import io.mango.infra.security.starter.aspect.PermAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Security auto-configuration.
 * <p>
 * Enables PermAspect for @Perm annotation permission checking.
 * IPermissionService resolution order:
 * <ol>
 *   <li>If {@code mango-rbac-starter} is on classpath → uses that (via its @Bean)</li>
 *   <li>Otherwise → falls back to {@link DefaultPermissionServiceImpl}</li>
 * </ol>
 *
 * @author Mango
 */
@AutoConfiguration
@ComponentScan(basePackages = "io.mango.infra.security.starter.aspect")
public class SecurityAutoConfiguration {

    /**
     * Default IPermissionService — only created when no other implementation exists.
     * This handles the case where mango-rbac-starter is not on classpath.
     */
    @Bean
    @ConditionalOnMissingBean(IPermissionService.class)
    public IPermissionService defaultPermissionService() {
        return new DefaultPermissionServiceImpl();
    }
}
