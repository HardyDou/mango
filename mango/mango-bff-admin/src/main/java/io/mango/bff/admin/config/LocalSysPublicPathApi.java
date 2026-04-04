package io.mango.bff.admin.config;

import io.mango.common.result.R;
import io.mango.gateway.api.SysPublicPathApi;
import io.mango.permission.core.service.ISysPublicPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Local implementation of SysPublicPathApi for BFF admin.
 * In BFF monolith mode, the gateway's Feign client cannot reach the permission service
 * via service discovery, so we provide a direct local implementation.
 */
@Configuration
@RequiredArgsConstructor
public class LocalSysPublicPathApi {

    private final ISysPublicPathService publicPathService;

    @Bean
    @Primary
    public SysPublicPathApi sysPublicPathApi() {
        return new SysPublicPathApi() {
            @Override
            public R<List<String>> getAnonymousPaths() {
                return R.ok(publicPathService.getAnonymousPaths());
            }

            @Override
            public R<List<String>> getLoginRequiredPaths() {
                return R.ok(publicPathService.getLoginRequiredPaths());
            }

            @Override
            public R<List<String>> listInternalPaths() {
                return R.ok(publicPathService.listInternalPaths());
            }

            @Override
            public R<Boolean> isPublicPath(String path) {
                return R.ok(publicPathService.isPublicPath(path));
            }
        };
    }
}