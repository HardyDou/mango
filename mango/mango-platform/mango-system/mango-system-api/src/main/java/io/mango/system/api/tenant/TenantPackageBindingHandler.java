package io.mango.system.api.tenant;

/**
 * 机构套餐绑定后的授权同步处理器。
 */
public interface TenantPackageBindingHandler {

    /**
     * 将机构绑定到指定套餐后，同步默认角色的菜单授权。
     */
    void bindPackage(Long tenantId, Long packageId);
}
