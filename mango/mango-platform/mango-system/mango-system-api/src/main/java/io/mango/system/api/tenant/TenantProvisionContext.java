package io.mango.system.api.tenant;

/**
 * 机构初始化上下文。
 * 底层仍使用 tenantId 表达机构空间隔离边界。
 *
 * @param tenantId 机构 ID，底层对应 tenantId
 * @param tenantCode 机构编码，底层对应 tenantCode
 * @param tenantName 机构名称，底层对应 tenantName
 */
public record TenantProvisionContext(Long tenantId, String tenantCode, String tenantName) {
}
