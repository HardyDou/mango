package io.mango.auth.starter.config;

import io.mango.authorization.api.ITokenProvider;
import io.mango.auth.core.store.TokenRevocationStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/** Registers the fail-closed security filter chain for the module diagnostic endpoint. */
@AutoConfiguration
@AutoConfigureAfter(ModuleDiagnosticAuthorizationAutoConfiguration.class)
public class ModuleDiagnosticSecurityAutoConfiguration {

    public static final String SECURITY_FILTER_CHAIN_BEAN_NAME = "mangoModuleDiagnosticSecurityFilterChain";

    @Bean(name = SECURITY_FILTER_CHAIN_BEAN_NAME)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnBean(name = ModuleDiagnosticAuthorizationManager.BEAN_NAME)
    @ConditionalOnProperty(name = "mango.module.diagnostics.endpoint.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "mango.access.auth-enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain mangoModuleDiagnosticSecurityFilterChain(
            HttpSecurity http,
            @Qualifier(ModuleDiagnosticAuthorizationManager.BEAN_NAME)
            AuthorizationManager<RequestAuthorizationContext> authorizationManager,
            ITokenProvider tokenService,
            ObjectProvider<TokenRevocationStore> tokenRevocationStoreProvider,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {
        AntPathRequestMatcher endpoint = new AntPathRequestMatcher("/actuator/mangoModules");
        http
                .securityMatcher(endpoint)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(new AntPathRequestMatcher("/actuator/mangoModules", "GET"))
                        .access(authorizationManager)
                        .anyRequest()
                        .denyAll())
                .addFilterBefore(new AuthSecurityConfig.AuthTokenAuthenticationFilter(
                                tokenService,
                                tokenRevocationStoreProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
