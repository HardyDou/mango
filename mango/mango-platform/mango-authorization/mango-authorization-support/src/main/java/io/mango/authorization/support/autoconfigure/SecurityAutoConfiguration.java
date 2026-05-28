package io.mango.authorization.support.autoconfigure;

import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.support.autoconfigure.context.SpringSecurityContextProvider;
import io.mango.authorization.support.autoconfigure.sensitive.AuthorizationSensitiveRawAccessProvider;
import io.mango.authorization.support.autoconfigure.web.JsonAccessDeniedHandler;
import io.mango.authorization.support.autoconfigure.web.JsonAuthenticationEntryPoint;
import io.mango.infra.sensitive.api.ISensitiveRawAccessProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * 安全能力自动配置。
 *
 * @author Mango
 */
@AutoConfiguration
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ISecurityContextProvider.class)
    public ISecurityContextProvider securityContextProvider() {
        return new SpringSecurityContextProvider();
    }

    @Bean
    @ConditionalOnMissingBean(ISensitiveRawAccessProvider.class)
    public ISensitiveRawAccessProvider sensitiveRawAccessProvider(
            ISecurityContextProvider securityContextProvider,
            ObjectProvider<IAuthorizationProvider> authorizationProviders) {
        return new AuthorizationSensitiveRawAccessProvider(
                securityContextProvider,
                authorizationProviders::getIfAvailable);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new JsonAuthenticationEntryPoint();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(AccessDeniedHandler.class)
    public AccessDeniedHandler accessDeniedHandler() {
        return new JsonAccessDeniedHandler();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    @ConditionalOnProperty(prefix = "mango.security.debug-permit-all-filter-chain", name = "enabled", havingValue = "true")
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
