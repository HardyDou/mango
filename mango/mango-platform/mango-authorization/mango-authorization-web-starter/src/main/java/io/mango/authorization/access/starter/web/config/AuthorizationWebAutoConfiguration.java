package io.mango.authorization.access.starter.web.config;

import io.mango.authorization.api.PublicPathApi;
import io.mango.authorization.access.core.auth.AccessService;
import io.mango.authorization.access.core.config.DynamicWhiteListConfig;
import io.mango.authorization.access.core.config.AccessProperties;
import io.mango.authorization.access.starter.web.filter.AuthFilter;
import io.mango.infra.security.api.ITokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 边界入口单体模式配置。
 *
 * @author Mango
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@EnableConfigurationProperties(AccessProperties.class)
public class AuthorizationWebAutoConfiguration {

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
    public FilterRegistrationBean<AuthFilter> authFilterRegistration(
            AccessService accessService) {
        AuthFilter authFilter = new AuthFilter(accessService);
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(authFilter);
        registration.addUrlPatterns("/*");
        registration.setName("authFilter");
        registration.setOrder(1); // 最高优先级
        return registration;
    }
}
