package io.mango.infra.log.layout;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.PriorityOrdered;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TraceId 提取过滤器
 * <p>
 * 从各种 APM 系统提取 traceId 并放入 MDC：
 * <ul>
 *   <li>SkyWalking: org.apache.skywalking.apm.toolkit.trace.TraceContext</li>
 *   <li>Micrometer/OTEL: io.micrometer.tracing.Tracer</li>
 *   <li>Zipkin: brave.Tracer</li>
 *   <li>自定义 Header: X-Trace-Id 等</li>
 * </ul>
 * </p>
 *
 * @author Mango
 */
public class TraceIdFilter implements Filter, PriorityOrdered {

    public static final String TRACE_ID_KEY = "traceId";
    private static final String DEFAULT_TRACE_ID = "-";

    private static final AtomicBoolean skywalkingAvailable = new AtomicBoolean(false);
    private static final AtomicBoolean micrometerAvailable = new AtomicBoolean(false);

    private final String customHeaderName;

    static {
        try {
            Class.forName("org.apache.skywalking.apm.toolkit.trace.TraceContext");
            skywalkingAvailable.set(true);
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class.forName("io.micrometer.tracing.Tracer");
            micrometerAvailable.set(true);
        } catch (ClassNotFoundException ignored) {
        }
    }

    public TraceIdFilter(String customHeaderName) {
        this.customHeaderName = customHeaderName;
    }

    public TraceIdFilter() {
        this(null);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String traceId = getTraceId((HttpServletRequest) request);
            if (traceId != null) {
                MDC.put(TRACE_ID_KEY, traceId);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String getTraceId(HttpServletRequest request) {
        // 1. 尝试从 MDC 获取（其他框架已放入的值）
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }

        // 2. 尝试从 SkyWalking 获取
        if (skywalkingAvailable.get()) {
            traceId = getSkyWalkingTraceId();
            if (traceId != null && !traceId.isEmpty()) {
                return traceId;
            }
        }

        // 3. 尝试从 Micrometer/OTEL 获取
        if (micrometerAvailable.get()) {
            traceId = getMicrometerTraceId();
            if (traceId != null && !traceId.isEmpty()) {
                return traceId;
            }
        }

        // 4. 从自定义 Header 获取
        if (customHeaderName != null && !customHeaderName.isEmpty()) {
            traceId = request.getHeader(customHeaderName);
            if (traceId != null && !traceId.isEmpty()) {
                return traceId;
            }
        }

        // 5. 尝试常见的 traceId header
        traceId = getCommonTraceIdHeader(request);
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }

        return DEFAULT_TRACE_ID;
    }

    private String getCommonTraceIdHeader(HttpServletRequest request) {
        // 常见 traceId header 顺序
        String[] headers = {
                "X-Trace-Id",
                "X-Request-Id",
                "X-Correlation-Id",
                "trace-id",
                "request-id"
        };
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private String getSkyWalkingTraceId() {
        try {
            Class<?> traceContextClass = Class.forName("org.apache.skywalking.apm.toolkit.trace.TraceContext");
            java.lang.reflect.Method method = traceContextClass.getMethod("getTraceId");
            Object result = method.invoke(null);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getMicrometerTraceId() {
        try {
            Class<?> tracerClass = Class.forName("io.micrometer.tracing.Tracer");
            java.lang.reflect.Method currentSpanMethod = tracerClass.getMethod("currentSpan");
            Object span = currentSpanMethod.invoke(null);
            if (span != null) {
                java.lang.reflect.Method contextMethod = span.getClass().getMethod("context");
                Object context = contextMethod.invoke(span);
                if (context != null) {
                    java.lang.reflect.Method traceIdMethod = context.getClass().getMethod("traceId");
                    Object traceId = traceIdMethod.invoke(context);
                    return traceId != null ? traceId.toString() : null;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE;
    }
}
