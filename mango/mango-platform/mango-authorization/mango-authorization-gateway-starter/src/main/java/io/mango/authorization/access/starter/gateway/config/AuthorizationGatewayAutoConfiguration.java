package io.mango.authorization.access.starter.gateway.config;

import io.mango.authorization.api.PublicPathApi;
import io.mango.authorization.access.core.auth.AccessService;
import io.mango.authorization.access.core.config.DynamicWhiteListConfig;
import io.mango.authorization.access.core.config.AccessProperties;
import io.mango.authorization.access.starter.gateway.filter.AuthGlobalFilter;
import io.mango.infra.security.api.ITokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 边界入口微服务模式配置。
 *
 * @author Mango
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@EnableConfigurationProperties(AccessProperties.class)
public class AuthorizationGatewayAutoConfiguration {

    private final AccessProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public DynamicWhiteListConfig dynamicWhiteListConfig(PublicPathApi publicPathApi) {
        return new DynamicWhiteListConfig(publicPathApi);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessService accessService(
            ITokenProvider tokenService,
            DynamicWhiteListConfig whiteListConfig) {
        return new AccessService(properties, tokenService, whiteListConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthGlobalFilter authGlobalFilter(AccessService accessService) {
        return new AuthGlobalFilter(accessService);
    }
}
