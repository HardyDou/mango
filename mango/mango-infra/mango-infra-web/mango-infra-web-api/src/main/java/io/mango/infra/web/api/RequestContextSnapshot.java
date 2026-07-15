package io.mango.infra.web.api;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.common.contract.LocalCapabilityContract;

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
@LocalCapabilityContract
@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Map components are defensively copied by the constructor")
public record RequestContextSnapshot(
        String requestId,
        String traceId,
        String clientIp,
        Object request,
        Map<String, String> headers,
        Map<String, String> cookies) {

    public RequestContextSnapshot {
        headers = immutableMap(headers);
        cookies = immutableMap(cookies);
    }

    /**
     * 为非 HTTP 执行路径创建空上下文。
     *
     * @return 空上下文
     */
    public static RequestContextSnapshot empty() {
        return new RequestContextSnapshot(null, null, null, null, Map.of(), Map.of());
    }

    private static Map<String, String> immutableMap(Map<String, String> values) {
        if (values == null) {
            return Map.of();
        }
        return Map.copyOf(values);
    }
}
