package io.mango.infra.context.core;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * Tenant context holder using TransmittableThreadLocal for thread-safe tenant ID propagation.
 * <p>
 * Unlike plain ThreadLocal, TransmittableThreadLocal propagates context across thread pool
 * boundaries (ScheduledExecutorService, CompletableFuture.runAsync(), etc.).
 *
 * @author Mango
 */
public class TenantContextHolder {

    private static final TransmittableThreadLocal<String> TENANT = new TransmittableThreadLocal<>();

    private TenantContextHolder() {
    }

    public static String getTenantId() {
        return TENANT.get();
    }

    public static void setTenantId(String tenantId) {
        TENANT.set(tenantId);
    }

    public static void clear() {
        TENANT.remove();
    }
}
