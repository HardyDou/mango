package io.mango.infra.persistence.api.scope;

import java.util.Optional;

/**
 * 租户提供者。
 * <p>
 * 用于后续租户隔离、租户字段填充和多租户插件扩展。
 */
public interface TenantProvider {

    /**
     * 获取当前租户标识。
     *
     * @return 当前租户标识。
     */
    Optional<String> currentTenantId();
}
