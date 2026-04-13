package io.mango.gateway.core.filter;

import io.mango.gateway.api.GatewayConstant;
import io.mango.gateway.core.config.DynamicWhiteListConfig;
import io.mango.gateway.core.config.GatewayProperties;
import io.mango.infra.security.api.ITokenService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

/**
 * 认证过滤器 (单体模式使用)
 * <p>
 * 基于Servlet Filter实现，兼容Spring MVC
 *
 * @author Mango
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AuthFilter implements Filter {

    private final GatewayProperties properties;
    private final ITokenService tokenService;
    private final DynamicWhiteListConfig whiteListConfig;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();

        // 白名单直接放行（优先使用动态配置）
        if (isWhiteList(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 检查是否启用认证
        if (!properties.isAuthEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        // 获取Token
        String authHeader = request.getHeader(GatewayConstant.TOKEN_HEADER);
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

        // 验证Token类型（拒绝refresh token作为bearer token）
        String tokenType = tokenService.getTokenType(token);
        if (!ITokenService.TOKEN_TYPE_ACCESS.equals(tokenType)) {
            unauthorized(response, "Invalid token type: bearer token must be access token");
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
     * 判断是否在白名单中（动态白名单 + 静态白名单）
     */
    private boolean isWhiteList(String path) {
        // 1. 优先使用动态白名单（从数据库加载）
        if (whiteListConfig.isPublicPath(path)) {
            return true;
        }
        // 2. 回退到静态白名单
        return isStaticWhiteList(path);
    }

    /**
     * 静态白名单匹配（向后兼容）
     */
    private boolean isStaticWhiteList(String path) {
        for (String pattern : GatewayConstant.WHITE_LIST) {
            if (matchPattern(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 通配符路径匹配
     */
    private boolean matchPattern(String pattern, String path) {
        if (pattern.equals(path)) {
            return true;
        }
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            int slashIndex = path.indexOf('/', prefix.length());
            return path.startsWith(prefix) && (slashIndex == -1 || slashIndex == path.length() - 1);
        }
        return false;
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
