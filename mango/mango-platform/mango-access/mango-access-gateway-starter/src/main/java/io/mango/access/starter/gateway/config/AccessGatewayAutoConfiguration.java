package io.mango.access.starter.gateway.config;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.access.core.auth.AccessContextValidator;
import io.mango.access.core.auth.AccessService;
import io.mango.access.core.config.AccessProperties;
import io.mango.access.starter.gateway.filter.AuthGlobalFilter;
import io.mango.authorization.api.ITokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 边界入口微服务模式配置。
 *
 * @author Mango
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AccessProperties.class)
public class AccessGatewayAutoConfiguration {

    private final AccessProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public AccessService accessService(
            ITokenProvider tokenService,
            ApiResourceApi apiResourceApi,
            IAuthorizationProvider authorizationProvider,
            ObjectProvider<AccessContextValidator> contextValidators) {
        return new AccessService(properties, tokenService, apiResourceApi, authorizationProvider,
                contextValidators.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthGlobalFilter authGlobalFilter(AccessService accessService) {
        return new AuthGlobalFilter(accessService);
    }
}
