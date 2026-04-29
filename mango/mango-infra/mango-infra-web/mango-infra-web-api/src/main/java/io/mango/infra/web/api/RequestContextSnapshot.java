package io.mango.infra.web.api;

import java.util.Map;

/**
 * 不可变 HTTP 请求上下文快照。
 *
 * @param requestId 来自可信请求元数据的请求标识
 * @param traceId 来自可信请求元数据的分布式链路标识
 * @param clientIp 解析后的客户端 IP 地址
 * @param request 底层 HTTP 请求对象；不可用时为空
 * @param headers 请求 Header
 * @param cookies 请求 Cookie
 */
public record RequestContextSnapshot(
        String requestId,
        String traceId,
        String clientIp,
        Object request,
        Map<String, String> headers,
        Map<String, String> cookies) {

    /**
     * 为非 HTTP 执行路径创建空上下文。
     *
     * @return 空上下文
     */
    public static RequestContextSnapshot empty() {
        return new RequestContextSnapshot(null, null, null, null, Map.of(), Map.of());
    }

    public RequestContextSnapshot {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        cookies = cookies == null ? Map.of() : Map.copyOf(cookies);
    }
}
