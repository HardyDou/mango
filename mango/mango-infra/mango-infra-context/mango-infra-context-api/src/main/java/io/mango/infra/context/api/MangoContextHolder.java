package io.mango.infra.context.api;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.function.UnaryOperator;

/**
 * Mango 运行时上下文持有器。
 * <p>
 * 使用 TransmittableThreadLocal 支持受托管线程池中的上下文传递。
 *
 * @author Mango
 */
public final class MangoContextHolder {

    private static final TransmittableThreadLocal<MangoContextSnapshot> CONTEXT = new TransmittableThreadLocal<>();

    private MangoContextHolder() {
    }

    public static MangoContextSnapshot get() {
        MangoContextSnapshot snapshot = CONTEXT.get();
        return snapshot == null ? MangoContextSnapshot.empty() : snapshot;
    }

    public static void set(MangoContextSnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            clear();
            return;
        }
        CONTEXT.set(snapshot);
    }

    public static void update(UnaryOperator<MangoContextSnapshot> updater) {
        if (updater == null) {
            return;
        }
        set(updater.apply(get()));
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static String requestId() {
        return get().requestId();
    }

    public static String traceId() {
        return get().traceId();
    }

    public static String tenantId() {
        return get().tenantId();
    }

    public static Long userId() {
        return get().userId();
    }

    public static Long memberId() {
        return get().memberId();
    }

    public static String principalName() {
        return get().principalName();
    }

    public static String appCode() {
        return get().appCode();
    }

    public static String clientIp() {
        return get().clientIp();
    }
}
