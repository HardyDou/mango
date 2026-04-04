package io.mango.common.context;

/**
 * Tenant context holder using ThreadLocal for thread-safe tenant ID propagation.
 *
 * @author Mango
 */
public class TenantContextHolder {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

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
