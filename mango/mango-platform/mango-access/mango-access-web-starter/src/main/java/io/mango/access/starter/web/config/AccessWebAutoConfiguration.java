package io.mango.access.starter.web.config;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.access.core.auth.AccessService;
import io.mango.access.core.config.AccessProperties;
import io.mango.access.starter.web.filter.AuthFilter;
import io.mango.authorization.api.ITokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 边界入口单体模式配置。
 *
 * @author Mango
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AccessProperties.class)
public class AccessWebAutoConfiguration {

    private final AccessProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public AccessService accessService(
            ITokenProvider tokenService,
            ApiResourceApi apiResourceApi,
            IAuthorizationProvider authorizationProvider) {
        return new AccessService(properties, tokenService, apiResourceApi, authorizationProvider);
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
