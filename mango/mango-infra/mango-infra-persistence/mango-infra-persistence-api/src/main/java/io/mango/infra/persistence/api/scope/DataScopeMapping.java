package io.mango.infra.persistence.api.scope;

import lombok.Builder;

/**
 * 业务表字段与数据权限语义的映射。
 *
 * @param tableName 数据权限作用的业务表名。传入后会在运行期校验所需字段是否存在。
 * @param selfField 归属当前主体的数据字段，例如 created_by。
 * @param orgField 归属组织的数据字段，例如 org_id。
 * @param tenantField 租户字段。租户隔离通常由租户插件统一处理，复杂 SQL 可显式传入。
 */
@Builder
public record DataScopeMapping(
        String tableName,
        String selfField,
        String orgField,
        String tenantField
) {
}
