package io.mango.infra.persistence.api.scope;

import java.util.Optional;

/**
 * 数据权限范围提供者。
 * <p>
 * 本接口只定义扩展点，不在基础设施层写死具体权限规则。
 */
public interface DataScopeProvider {

    /**
     * 解析指定资源的数据权限范围。
     *
     * @param resourceCode 资源编码。
     * @return 数据权限范围。
     */
    Optional<DataScopeRule> resolve(String resourceCode);
}
