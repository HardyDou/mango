package io.mango.access.starter.gateway.filter;

import io.mango.access.core.AccessConstants;
import io.mango.access.api.vo.AccessPrincipalVO;
import io.mango.access.api.vo.AccessResultVO;
import io.mango.access.core.auth.AccessEvaluator;
import io.mango.infra.context.api.MangoContextHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

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

    private final Supplier<AccessEvaluator> accessEvaluatorSupplier;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        ServerHttpRequest sanitizedRequest = clearSecurityHeaders(request);
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();
        if (isRealtimeTicketPath(path) && hasText(request.getQueryParams().getFirst("rtTicket"))) {
            return chain.filter(sanitizedExchange);
        }
        return Mono.fromCallable(() -> accessEvaluatorSupplier.get().check(
                        request.getMethod().name(), path, resolveTokenCredential(request),
                        resolveRemoteAddress(request)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> forward(sanitizedExchange, chain, result));
    }

    private Mono<Void> forward(ServerWebExchange exchange, GatewayFilterChain chain, AccessResultVO result) {
        if (result.status() == AccessResultVO.Status.FORBIDDEN) {
            return forbidden(exchange, result.message());
        }
        if (result.status() == AccessResultVO.Status.UNAUTHORIZED) {
            return unauthorized(exchange, result.message());
        }
        if (result.status() == AccessResultVO.Status.SERVICE_UNAVAILABLE) {
            return error(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                    HttpStatus.SERVICE_UNAVAILABLE.value(), result.message());
        }
        if (result.principal() == null) {
            return chain.filter(exchange);
        }

        ServerHttpRequest mutatedRequest = writePrincipalHeaders(exchange.getRequest(), result.principal());

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private ServerHttpRequest clearSecurityHeaders(ServerHttpRequest request) {
        return request.mutate().headers(headers -> {
            headers.remove(MangoContextHeaders.TENANT_ID);
            headers.remove(MangoContextHeaders.USER_ID);
            headers.remove(MangoContextHeaders.MEMBER_ID);
            headers.remove(MangoContextHeaders.PRINCIPAL_NAME);
            headers.remove(MangoContextHeaders.REALM);
            headers.remove(MangoContextHeaders.ACTOR_TYPE);
            headers.remove(MangoContextHeaders.PARTY_TYPE);
            headers.remove(MangoContextHeaders.PARTY_ID);
            headers.remove(MangoContextHeaders.APP_CODE);
        }).build();
    }

    private boolean isRealtimeTicketPath(String path) {
        return "/realtime/transports/websocket".equals(path)
                || "/realtime/transports/sse".equals(path)
                || path != null && path.startsWith("/realtime/transports/probe/");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveTokenCredential(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(AccessConstants.TOKEN_HEADER);
        if (authHeader != null && !authHeader.isBlank()) {
            return authHeader;
        }
        String queryToken = request.getQueryParams().getFirst("token");
        if (queryToken != null && !queryToken.isBlank()) {
            String trimmed = queryToken.trim();
            return trimmed.startsWith("Bearer ") ? trimmed : "Bearer " + trimmed;
        }
        String token = request.getCookies().getFirst("MANGO_TOKEN") == null
                ? null
                : request.getCookies().getFirst("MANGO_TOKEN").getValue();
        if (token == null || token.isBlank()) {
            return null;
        }
        String trimmed = token.trim();
        return trimmed.startsWith("Bearer ") ? trimmed : "Bearer " + trimmed;
    }

    @Override
    public int getOrder() {
        return -100; // 优先级最高
    }

    private ServerHttpRequest writePrincipalHeaders(ServerHttpRequest request, AccessPrincipalVO principal) {
        ServerHttpRequest.Builder builder = request.mutate();
        put(builder, MangoContextHeaders.USER_ID, principal.userId());
        put(builder, MangoContextHeaders.MEMBER_ID, principal.memberId());
        put(builder, MangoContextHeaders.PRINCIPAL_NAME, principal.username());
        put(builder, MangoContextHeaders.TENANT_ID, principal.tenantId());
        put(builder, MangoContextHeaders.REALM, principal.realm());
        put(builder, MangoContextHeaders.ACTOR_TYPE, principal.actorType());
        put(builder, MangoContextHeaders.PARTY_TYPE, principal.partyType());
        put(builder, MangoContextHeaders.PARTY_ID, principal.partyId());
        put(builder, MangoContextHeaders.APP_CODE, principal.appCode());
        return builder.build();
    }

    private String resolveRemoteAddress(ServerHttpRequest request) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress == null || remoteAddress.getAddress() == null
                ? null
                : remoteAddress.getAddress().getHostAddress();
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
        return error(exchange, HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.value(), message);
    }

    /**
     * 返回禁止访问响应（内部API不允许外部访问）
     */
    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        return error(exchange, HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.value(), message);
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, int code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String safe = message == null ? "" : message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        String body = "{\"code\":" + code + ",\"message\":\"" + safe + "\"}";
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8)))
        );
    }
}
