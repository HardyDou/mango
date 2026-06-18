package io.mango.infra.persistence.api.scope;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

/**
 * 数据权限条件应用器。
 */
public interface DataScopeApplier {

    /**
     * 将指定资源的数据权限应用到查询条件。
     *
     * @param wrapper 查询条件。
     * @param resourceCode 资源编码。
     * @param mapping 业务字段映射。
     * @param <T> 查询实体类型。
     */
    <T> void apply(QueryWrapper<T> wrapper, String resourceCode, DataScopeMapping mapping);
}
