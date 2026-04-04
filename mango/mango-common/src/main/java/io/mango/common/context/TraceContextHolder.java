package io.mango.common.context;

/**
 * Trace context holder using ThreadLocal for thread-safe trace ID propagation.
 *
 * @author Mango
 */
public class TraceContextHolder {

    private static final ThreadLocal<String> TRACE = new ThreadLocal<>();

    private TraceContextHolder() {
    }

    public static String getTraceId() {
        return TRACE.get();
    }

    public static void setTraceId(String traceId) {
        TRACE.set(traceId);
    }

    public static void clear() {
        TRACE.remove();
    }
}
