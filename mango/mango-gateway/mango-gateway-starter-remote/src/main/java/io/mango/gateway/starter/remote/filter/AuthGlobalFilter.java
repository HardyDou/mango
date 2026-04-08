package io.mango.gateway.starter.remote.filter;

import io.mango.gateway.api.GatewayConstant;
import io.mango.gateway.core.config.DynamicWhiteListConfig;
import io.mango.gateway.core.config.GatewayProperties;
import io.mango.infra.security.api.ITokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 认证全局过滤器 (微服务模式)
 * <p>
 * 验证请求Token有效性，并将用户信息传递给下游服务
 *
 * @author Mango
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final GatewayProperties properties;
    private final ITokenService tokenService;
    private final DynamicWhiteListConfig whiteListConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 内部路径直接拒绝（403）
        if (whiteListConfig.isInternalPath(path)) {
            return forbidden(exchange, "Internal API not accessible");
        }

        // 白名单直接放行
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        // 检查是否启用认证
        if (!properties.isAuthEnabled()) {
            return chain.filter(exchange);
        }

        // 获取Token
        String authHeader = request.getHeaders().getFirst(GatewayConstant.TOKEN_HEADER);
        if (authHeader == null || !authHeader.startsWith(ITokenService.BEARER_PREFIX)) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(ITokenService.BEARER_PREFIX.length());

        // 验证Token
        if (!tokenService.validateToken(token)) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        // 验证Token类型（拒绝refresh token作为bearer token）
        String tokenType = tokenService.getTokenType(token);
        if (!ITokenService.TOKEN_TYPE_ACCESS.equals(tokenType)) {
            return unauthorized(exchange, "Invalid token type: bearer token must be access token");
        }

        // 获取用户信息
        Long userId = tokenService.getUserId(token);
        String username = tokenService.getUsername(token);

        // 构建转发的请求头
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(GatewayConstant.USER_ID_HEADER, String.valueOf(userId))
                .header("X-Username", username)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100; // 优先级最高
    }

    /**
     * 判断是否在静态白名单中（向后兼容）
     */
    private boolean isWhiteList(String path) {
        // Use dynamic white list first
        if (whiteListConfig.isPublicPath(path)) {
            return true;
        }
        // Fallback to static white list
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
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String body = "{\"code\":401,\"message\":\"" + message + "\"}";
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes()))
        );
    }

    /**
     * 返回禁止访问响应（内部API不允许外部访问）
     */
    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String body = "{\"code\":403,\"message\":\"" + message + "\"}";
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes()))
        );
    }
}
