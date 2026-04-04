package io.mango.gateway.starter.config;

import io.mango.gateway.core.config.GatewayProperties;
import io.mango.gateway.core.filter.AuthFilter;
import io.mango.gateway.core.util.JwtUtil;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway单体模式配置
 * <p>
 * 当未启用服务发现时，提供基于Servlet Filter的认证
 *
 * @author Mango
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayStarterConfig {

    private final GatewayProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public JwtUtil jwtUtil() {
        return new JwtUtil(properties.getJwtSecret(), properties.getTokenExpireSeconds() * 1000);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthFilter authFilter(JwtUtil jwtUtil) {
        return new AuthFilter(properties, jwtUtil);
    }

    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<AuthFilter> authFilterRegistration(AuthFilter authFilter) {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(authFilter);
        registration.addUrlPatterns("/*");
        registration.setName("authFilter");
        registration.setOrder(1); // 最高优先级
        return registration;
    }
}
