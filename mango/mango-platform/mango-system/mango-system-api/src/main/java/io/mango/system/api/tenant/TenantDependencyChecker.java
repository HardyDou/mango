package io.mango.system.api.tenant;

import java.util.Optional;

/**
 * 机构删除依赖检查扩展点。
 * <p>
 * 各模块只检查自己拥有的数据，系统模块负责汇总后决定机构是否允许删除。
 */
public interface TenantDependencyChecker {

    /**
     * 检查指定机构是否存在本模块业务数据依赖。
     *
     * @param tenantId 机构 ID，底层对应 tenantId。
     * @return 存在依赖时返回阻断原因；无依赖时返回空。
     */
    Optional<String> check(Long tenantId);
}
