package io.mango.auth.starter.config;

import io.mango.auth.core.constant.AuthConstant;
import io.mango.infra.security.api.ITokenService;
import io.mango.infra.security.api.SecurityPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Auth security configuration.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuthSecurityConfig {

    private final ITokenService tokenService;

    @Bean
    @ConditionalOnProperty(name = "mango.gateway.auth-enabled", havingValue = "true", matchIfMissing = true)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authSecurityFilterChain(
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
                .authorizeHttpRequests(authorize -> {
                    for (String path : AuthConstant.WHITE_LIST) {
                        authorize.requestMatchers(path).permitAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                .addFilterBefore(new AuthTokenAuthenticationFilter(tokenService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Slf4j
    static class AuthTokenAuthenticationFilter extends OncePerRequestFilter {

        private final ITokenService tokenService;

        AuthTokenAuthenticationFilter(ITokenService tokenService) {
            this.tokenService = tokenService;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            if (isWhiteList(request.getRequestURI())) {
                filterChain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader(AuthConstant.TOKEN_HEADER);
            if (authHeader != null && authHeader.startsWith(ITokenService.BEARER_PREFIX)) {
                String token = authHeader.substring(ITokenService.BEARER_PREFIX.length());
                if (tokenService.validateToken(token)
                        && ITokenService.TOKEN_TYPE_ACCESS.equals(tokenService.getTokenType(token))) {
                    Long userId = tokenService.getUserId(token);
                    String username = tokenService.getUsername(token);
                    SecurityPrincipal principal = new SecurityPrincipal(userId, null, username);
                    UsernamePasswordAuthenticationToken authentication =
                            UsernamePasswordAuthenticationToken.authenticated(
                                    principal,
                                    token,
                                    AuthorityUtils.NO_AUTHORITIES);
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                }
            }

            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        private boolean isWhiteList(String path) {
            return Arrays.stream(AuthConstant.WHITE_LIST)
                    .anyMatch(pattern -> {
                        if (pattern.endsWith("/**")) {
                            String prefix = pattern.substring(0, pattern.length() - 3);
                            return path.startsWith(prefix);
                        }
                        return path.equals(pattern);
                    });
        }
    }
}
