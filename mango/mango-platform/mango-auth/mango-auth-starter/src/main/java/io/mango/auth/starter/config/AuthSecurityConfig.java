package io.mango.auth.starter.config;

import io.mango.auth.core.constant.AuthConstant;
import io.mango.auth.core.service.TokenRevocationService;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.security.api.ITokenProvider;
import io.mango.infra.security.api.SecurityPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 认证安全配置。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuthSecurityConfig {

    private final ITokenProvider tokenService;
    private final ObjectProvider<TokenRevocationService> tokenRevocationServiceProvider;

    @Bean
    @ConditionalOnProperty(name = "mango.access.auth-enabled", havingValue = "true", matchIfMissing = true)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authSecurityFilterChain(
            HttpSecurity http,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler,
            @Qualifier("apiResourceAuthorizationManager")
            ObjectProvider<AuthorizationManager<RequestAuthorizationContext>> apiResourceAuthorizationManagerProvider)
            throws Exception {
        AuthorizationManager<RequestAuthorizationContext> apiResourceAuthorizationManager =
                apiResourceAuthorizationManagerProvider.getIfAvailable();
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> {
                    if (apiResourceAuthorizationManager == null) {
                        authorize.anyRequest().authenticated();
                    } else {
                        authorize.anyRequest().access(apiResourceAuthorizationManager);
                    }
                })
                .addFilterBefore(new AuthTokenAuthenticationFilter(
                        tokenService,
                        tokenRevocationServiceProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Slf4j
    static class AuthTokenAuthenticationFilter extends OncePerRequestFilter {

        private final ITokenProvider tokenService;
        private final ObjectProvider<TokenRevocationService> tokenRevocationServiceProvider;

        AuthTokenAuthenticationFilter(ITokenProvider tokenService,
                                      ObjectProvider<TokenRevocationService> tokenRevocationServiceProvider) {
            this.tokenService = tokenService;
            this.tokenRevocationServiceProvider = tokenRevocationServiceProvider;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String authHeader = request.getHeader(AuthConstant.TOKEN_HEADER);
            if (authHeader != null && authHeader.startsWith(ITokenProvider.BEARER_PREFIX)) {
                String token = authHeader.substring(ITokenProvider.BEARER_PREFIX.length());
                if (tokenService.validateToken(token)
                        && !isRevoked(token)
                        && ITokenProvider.TOKEN_TYPE_ACCESS.equals(tokenService.getTokenType(token))) {
                    Long userId = tokenService.getUserId(token);
                    String username = tokenService.getUsername(token);
                    String realm = tokenService.getClaim(token, "realm");
                    String actorType = tokenService.getClaim(token, "actorType");
                    String partyType = tokenService.getClaim(token, "partyType");
                    Long partyId = resolveLongClaim(token, "partyId");
                    String appCode = tokenService.getClaim(token, "appCode");
                    String tenantId = firstText(tokenService.getClaim(token, "tenantId"), MangoContextHolder.tenantId());
                    SecurityPrincipal principal = new SecurityPrincipal(
                            userId,
                            tenantId,
                            username,
                            realm,
                            actorType,
                            partyType,
                            partyId,
                            appCode);
                    UsernamePasswordAuthenticationToken authentication =
                            UsernamePasswordAuthenticationToken.authenticated(
                                    principal,
                                    token,
                                    AuthorityUtils.NO_AUTHORITIES);
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                    MangoContextHolder.update(current -> current.withSecurity(
                            userId,
                            tenantId,
                            username,
                            realm,
                            actorType,
                            partyType,
                            partyId,
                            appCode));
                }
            }

            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        private Long resolveLongClaim(String token, String claimName) {
            String value = tokenService.getClaim(token, claimName);
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private boolean isRevoked(String token) {
            TokenRevocationService tokenRevocationService = tokenRevocationServiceProvider.getIfAvailable();
            return tokenRevocationService != null && tokenRevocationService.isRevoked(token);
        }

        private String firstText(String first, String second) {
            if (first != null && !first.isBlank()) {
                return first.trim();
            }
            return second != null && !second.isBlank() ? second.trim() : null;
        }
    }
}
