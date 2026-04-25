package io.mango.infra.web.support;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

/**
 * Resolves trace id from APM context and HTTP headers.
 */
public class WebTraceIdResolver {

    public static final String TRACE_ID_KEY = "traceId";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    private static final String HEADER_TRACE_ID_LEGACY = "TRACE-ID";
    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_TRACEPARENT = "traceparent";

    public String resolveTraceId(HttpServletRequest request) {
        String traceId = firstText(MDC.get(TRACE_ID_KEY), skyWalkingTraceId());
        if (hasText(traceId)) {
            return traceId;
        }
        String traceparent = request.getHeader(HEADER_TRACEPARENT);
        traceId = traceIdFromTraceparent(traceparent);
        if (hasText(traceId)) {
            return traceId;
        }
        return firstText(
                request.getHeader(HEADER_TRACE_ID),
                request.getHeader(HEADER_TRACE_ID_LEGACY),
                request.getHeader(HEADER_REQUEST_ID));
    }

    private String traceIdFromTraceparent(String traceparent) {
        if (!hasText(traceparent)) {
            return null;
        }
        String[] parts = traceparent.split("-");
        if (parts.length >= 2 && parts[1].length() == 32) {
            return parts[1];
        }
        return null;
    }

    private String skyWalkingTraceId() {
        try {
            Class<?> traceContextClass = Class.forName("org.apache.skywalking.apm.toolkit.trace.TraceContext");
            Object result = traceContextClass.getMethod("getTraceId").invoke(null);
            return result == null ? null : result.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !"-".equals(value);
    }
}
