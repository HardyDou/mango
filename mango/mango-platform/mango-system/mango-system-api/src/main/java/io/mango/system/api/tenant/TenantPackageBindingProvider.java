package io.mango.system.api.tenant;

import java.util.List;

/**
 * 机构套餐绑定查询能力。
 */
public interface TenantPackageBindingProvider {

    /**
     * 查询绑定了指定套餐的机构 ID。
     */
    List<Long> listTenantIdsByPackage(Long packageId);
}
