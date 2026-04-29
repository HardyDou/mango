package io.mango.authorization.access.starter.gateway.filter;

import io.mango.authorization.access.core.AccessConstants;
import io.mango.authorization.access.core.auth.AccessResult;
import io.mango.authorization.access.core.auth.AccessService;
import io.mango.authorization.access.core.auth.AccessPrincipal;
import io.mango.infra.context.core.MangoContextHeaders;
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

import java.nio.charset.StandardCharsets;

/**
 * 认证全局过滤器（微服务模式）。
 * <p>
 * 验证请求 Token 有效性，并将主体上下文传递给下游服务。
 *
 * @author Mango
 */
@Slf4j
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AccessService gatewayAccessService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        AccessResult result = gatewayAccessService.check(path, request.getHeaders().getFirst(AccessConstants.TOKEN_HEADER));

        if (result.status() == AccessResult.Status.FORBIDDEN) {
            return forbidden(exchange, result.message());
        }
        if (result.status() == AccessResult.Status.UNAUTHORIZED) {
            return unauthorized(exchange, result.message());
        }
        if (result.principal() == null) {
            return chain.filter(exchange);
        }

        ServerHttpRequest mutatedRequest = writePrincipalHeaders(request, result.principal());

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100; // 优先级最高
    }

    private ServerHttpRequest writePrincipalHeaders(ServerHttpRequest request, AccessPrincipal principal) {
        ServerHttpRequest.Builder builder = request.mutate();
        put(builder, MangoContextHeaders.USER_ID, principal.userId());
        put(builder, MangoContextHeaders.PRINCIPAL_NAME, principal.username());
        put(builder, MangoContextHeaders.TENANT_ID, principal.tenantId());
        put(builder, MangoContextHeaders.REALM, principal.realm());
        put(builder, MangoContextHeaders.ACTOR_TYPE, principal.actorType());
        put(builder, MangoContextHeaders.PARTY_TYPE, principal.partyType());
        put(builder, MangoContextHeaders.PARTY_ID, principal.partyId());
        put(builder, MangoContextHeaders.APP_CODE, principal.appCode());
        return builder.build();
    }

    private void put(ServerHttpRequest.Builder builder, String name, Object value) {
        if (value != null && !value.toString().isBlank()) {
            builder.header(name, value.toString());
        }
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String body = "{\"code\":401,\"message\":\"" + message + "\"}";
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8)))
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
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8)))
        );
    }
}
