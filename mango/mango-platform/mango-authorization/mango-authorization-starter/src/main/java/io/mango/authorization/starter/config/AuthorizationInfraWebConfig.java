package io.mango.authorization.starter.config;

import io.mango.infra.web.api.IInternalPathProvider;
import io.mango.authorization.core.service.IPublicPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * infra-web 集成配置。
 * <p>
 * 提供 IInternalPathProvider 实现，将内部路径查询委托给授权模块的
 * IPublicPathService，打通 infra-web 与平台业务配置。
 * </p>
 *
 * @author Mango
 */
@Configuration
@RequiredArgsConstructor
public class AuthorizationInfraWebConfig {

    private final IPublicPathService publicPathService;

    /**
     * 注册 IInternalPathProvider 实现。
     */
    @Bean
    public IInternalPathProvider internalPathProvider() {
        return new IInternalPathProvider() {
            @Override
            public List<String> getInternalPaths() {
                return publicPathService.listInternalPaths();
            }
        };
    }
}
