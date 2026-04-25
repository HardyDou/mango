package io.mango.infra.web.api;

import java.util.Map;

/**
 * Immutable HTTP request context snapshot.
 *
 * @param requestId request identifier from trusted request metadata
 * @param traceId distributed trace identifier from trusted request metadata
 * @param clientIp resolved client IP address
 * @param request underlying HTTP request object, if available
 * @param headers request headers
 * @param cookies request cookies
 */
public record RequestContextSnapshot(
        String requestId,
        String traceId,
        String clientIp,
        Object request,
        Map<String, String> headers,
        Map<String, String> cookies) {

    /**
     * Create an empty context for non-HTTP execution paths.
     *
     * @return empty context
     */
    public static RequestContextSnapshot empty() {
        return new RequestContextSnapshot(null, null, null, null, Map.of(), Map.of());
    }

    public RequestContextSnapshot {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        cookies = cookies == null ? Map.of() : Map.copyOf(cookies);
    }
}
