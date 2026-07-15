package io.mango.authorization.starter.resource;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;

import java.util.function.Supplier;

/** Runs tenant-owned resource synchronization with the declaration tenant in scope. */
final class ResourceTenantScope {

    private ResourceTenantScope() {
    }

    static <T> T call(Long tenantId, Supplier<T> action) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withTenantId(String.valueOf(tenantId)));
            return action.get();
        } finally {
            MangoContextHolder.set(previous);
        }
    }
}
