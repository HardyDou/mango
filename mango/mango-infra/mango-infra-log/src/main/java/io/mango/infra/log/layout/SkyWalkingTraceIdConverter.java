package io.mango.infra.log.layout;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SkyWalking TraceId Converter
 * <p>
 * 支持从 SkyWalking 获取 traceId，如果 SkyWalking 未启用则返回默认值
 * </p>
 *
 * @author Mango
 */
public class SkyWalkingTraceIdConverter extends CompositeConverter<ILoggingEvent> {

    private static final String DEFAULT_TRACE_ID = "-";
    private static final AtomicBoolean skywalkingAvailable = new AtomicBoolean(false);

    static {
        try {
            Class.forName("org.apache.skywalking.apm.toolkit.trace.TraceContext");
            skywalkingAvailable.set(true);
        } catch (ClassNotFoundException e) {
            // SkyWalking 未引入，忽略
        }
    }

    @Override
    protected String transform(ILoggingEvent event, String in) {
        if (!skywalkingAvailable.get()) {
            return DEFAULT_TRACE_ID;
        }
        return getTraceId();
    }

    private String getTraceId() {
        try {
            Class<?> traceContextClass = Class.forName("org.apache.skywalking.apm.toolkit.trace.TraceContext");
            java.lang.reflect.Method method = traceContextClass.getMethod("getTraceId");
            Object result = method.invoke(null);
            return result != null ? result.toString() : DEFAULT_TRACE_ID;
        } catch (Exception e) {
            return DEFAULT_TRACE_ID;
        }
    }
}
