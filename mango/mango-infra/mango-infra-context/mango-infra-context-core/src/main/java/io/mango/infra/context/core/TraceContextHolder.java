package io.mango.infra.context.core;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * Trace context holder using TransmittableThreadLocal for thread-safe trace ID propagation.
 * <p>
 * Unlike plain ThreadLocal, TransmittableThreadLocal propagates context across thread pool
 * boundaries (ScheduledExecutorService, CompletableFuture.runAsync(), etc.).
 *
 * @author Mango
 */
public class TraceContextHolder {

    private static final TransmittableThreadLocal<String> TRACE = new TransmittableThreadLocal<>();

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
