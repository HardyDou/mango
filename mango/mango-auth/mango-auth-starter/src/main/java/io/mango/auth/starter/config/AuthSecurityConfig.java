package io.mango.auth.starter.config;

import io.mango.auth.core.constant.AuthConstant;
import io.mango.infra.security.api.ITokenService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.Arrays;

/**
 * 认证安全配置
 * <p>
 * 启用JWT认证，保护所有接口（除白名单外）
 *
 * @author Mango
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuthSecurityConfig {

    private final ITokenService tokenService;

    /**
     * 认证过滤器
     */
    @Bean("authAuthFilterRegistration")
    @ConditionalOnProperty(name = "mango.gateway.auth-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<AuthFilterBean> authAuthFilterRegistration() {
        FilterRegistrationBean<AuthFilterBean> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthFilterBean(tokenService));
        registration.addUrlPatterns("/*");
        registration.setName("authAuthFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * 认证过滤器实现
     */
    @Slf4j
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public static class AuthFilterBean implements Filter {

        private final ITokenService tokenService;

        public AuthFilterBean(ITokenService tokenService) {
            this.tokenService = tokenService;
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            String path = request.getRequestURI();

            // 白名单直接放行
            if (isWhiteList(path)) {
                chain.doFilter(request, response);
                return;
            }

            // 获取Token
            String authHeader = request.getHeader(AuthConstant.TOKEN_HEADER);
            if (authHeader == null || !authHeader.startsWith(ITokenService.BEARER_PREFIX)) {
                unauthorized(response, "Missing or invalid Authorization header");
                return;
            }

            String token = authHeader.substring(ITokenService.BEARER_PREFIX.length());

            // 验证Token
            if (!tokenService.validateToken(token)) {
                unauthorized(response, "Invalid or expired token");
                return;
            }

            // 验证Token类型：只允许access token，refresh token不能用于认证
            String tokenType = tokenService.getTokenType(token);
            if (!ITokenService.TOKEN_TYPE_ACCESS.equals(tokenType)) {
                unauthorized(response, "Invalid token type: expected access token");
                return;
            }

            // 获取用户信息
            Long userId = tokenService.getUserId(token);
            String username = tokenService.getUsername(token);

            // 设置到请求属性中，供后续Controller使用
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);

            chain.doFilter(request, response);
        }

        /**
         * 判断是否在白名单中
         */
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

        /**
         * 返回未授权响应
         */
        private void unauthorized(HttpServletResponse response, String message) throws IOException {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
        }
    }
}
