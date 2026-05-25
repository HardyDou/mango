package io.mango.infra.web.filter;

import io.mango.infra.context.core.MangoContextHeaders;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.web.api.IRequestContextProvider;
import io.mango.infra.web.api.RequestContextSnapshot;
import io.mango.infra.web.support.WebTraceIdResolver;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 将 HTTP 请求上下文初始化为 Mango 运行时上下文。
 *
 * @author Mango
 */
public class MangoContextWebFilter implements Filter {

    private static final String HEADER_REQUEST_ID = MangoContextHeaders.REQUEST_ID;
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_REAL_IP = "X-Real-IP";

    private final IRequestContextProvider requestContextProvider;
    private final WebTraceIdResolver traceIdResolver;

    public MangoContextWebFilter(IRequestContextProvider requestContextProvider) {
        this.requestContextProvider = Objects.requireNonNull(requestContextProvider, "requestContextProvider");
        this.traceIdResolver = new WebTraceIdResolver();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            RequestContextSnapshot requestContext = currentContext(request);
            MangoContextHolder.set(MangoContextSnapshot.request(
                    requestContext.requestId(),
                    requestContext.traceId(),
                    header(requestContext.headers(), MangoContextHeaders.TENANT_ID),
                    header(requestContext.headers(), MangoContextHeaders.APP_CODE),
                    requestContext.clientIp()));
            chain.doFilter(request, response);
        } finally {
            MangoContextHolder.clear();
        }
    }

    private RequestContextSnapshot currentContext(ServletRequest request) {
        if (request instanceof HttpServletRequest httpRequest) {
            return new RequestContextSnapshot(
                    firstPresent(httpRequest.getHeader(HEADER_REQUEST_ID), httpRequest.getRequestId()),
                    traceIdResolver.resolveTraceId(httpRequest),
                    resolveClientIp(httpRequest),
                    httpRequest,
                    readHeaders(httpRequest),
                    Map.of());
        }
        return requestContextProvider.currentContext();
    }

    private Map<String, String> readHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> headers.put(name, request.getHeader(name)));
        return headers;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(HEADER_FORWARDED_FOR);
        if (hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader(HEADER_REAL_IP);
        if (hasText(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private String firstPresent(String first, String second) {
        return hasText(first) ? first : second;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String header(Map<String, String> headers, String name) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        String value = headers.get(name);
        if (value != null) {
            return value;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
