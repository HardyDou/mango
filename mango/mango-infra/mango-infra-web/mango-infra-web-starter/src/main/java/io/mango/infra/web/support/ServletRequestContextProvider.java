package io.mango.infra.web.support;

import io.mango.infra.context.core.MangoContextHeaders;
import io.mango.infra.web.api.IRequestContextProvider;
import io.mango.infra.web.api.RequestContextSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于 Servlet 的 HTTP 请求上下文提供器。
 */
public class ServletRequestContextProvider implements IRequestContextProvider {

    private static final String HEADER_REQUEST_ID = MangoContextHeaders.REQUEST_ID;
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_REAL_IP = "X-Real-IP";
    private final WebTraceIdResolver traceIdResolver;

    public ServletRequestContextProvider(WebTraceIdResolver traceIdResolver) {
        this.traceIdResolver = traceIdResolver;
    }

    @Override
    public RequestContextSnapshot currentContext() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return RequestContextSnapshot.empty();
        }

        HttpServletRequest request = attributes.getRequest();
        return new RequestContextSnapshot(
                firstPresent(request.getHeader(HEADER_REQUEST_ID), request.getRequestId()),
                traceIdResolver.resolveTraceId(request),
                resolveClientIp(request),
                request,
                readHeaders(request),
                readCookies(request));
    }

    private Map<String, String> readHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> headers.put(name, request.getHeader(name)));
        return headers;
    }

    private Map<String, String> readCookies(HttpServletRequest request) {
        Map<String, String> cookies = new HashMap<>();
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                cookies.put(cookie.getName(), cookie.getValue());
            }
        }
        return cookies;
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
}
