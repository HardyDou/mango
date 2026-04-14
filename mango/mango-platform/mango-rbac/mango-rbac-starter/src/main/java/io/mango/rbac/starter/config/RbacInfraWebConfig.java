package io.mango.rbac.starter.config;

import io.mango.infra.web.api.IInternalPathProvider;
import io.mango.rbac.core.service.ISysPublicPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for infra-web integration.
 * <p>
 * Provides IInternalPathProvider implementation that delegates to RBAC's
 * ISysPublicPathService, bridging infra-web with platform business logic.
 * </p>
 *
 * @author Mango
 */
@Configuration
@RequiredArgsConstructor
public class RbacInfraWebConfig {

    private final ISysPublicPathService sysPublicPathService;

    /**
     * Register IInternalPathProvider implementation
     */
    @Bean
    public IInternalPathProvider internalPathProvider() {
        return new IInternalPathProvider() {
            @Override
            public List<String> getInternalPaths() {
                return sysPublicPathService.listInternalPaths();
            }
        };
    }
}
