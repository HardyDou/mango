package io.mango.auth.api.spi;

import io.mango.auth.api.vo.LoginTenantVO;

import java.util.List;

/**
 * 登录机构查询 Provider。
 * 认证模块只依赖机构登录契约，具体数据来源由系统模块提供。
 */
public interface LoginTenantProvider {

    /**
     * 按机构 ID 查询启用机构。
     *
     * @param tenantId 机构 ID，底层对应 tenantId。
     * @return 启用机构，不存在或禁用时返回 null。
     */
    LoginTenantVO getEnabledById(String tenantId);

    /**
     * 按机构编码查询启用机构。
     *
     * @param tenantCode 机构编码，底层对应 tenantCode。
     * @return 启用机构，不存在或禁用时返回 null。
     */
    LoginTenantVO getEnabledByCode(String tenantCode);

    /**
     * 按账号和机构 ID 查询当前账号可进入的启用机构。
     *
     * @param userId 全局账号 ID。
     * @param tenantId 机构 ID，底层对应 tenantId。
     * @return 当前账号可进入的机构，不存在、禁用或未加入时返回 null。
     */
    default LoginTenantVO getEnabledByUserAndTenantId(Long userId, String tenantId) {
        return getEnabledById(tenantId);
    }

    /**
     * 按账号和机构编码查询当前账号可进入的启用机构。
     *
     * @param userId 全局账号 ID。
     * @param tenantCode 机构编码，底层对应 tenantCode。
     * @return 当前账号可进入的机构，不存在、禁用或未加入时返回 null。
     */
    default LoginTenantVO getEnabledByUserAndTenantCode(Long userId, String tenantCode) {
        return getEnabledByCode(tenantCode);
    }

    /**
     * 查询账号可进入的启用机构选项。
     *
     * @param userId 全局账号 ID。
     * @return 可进入的启用机构选项。
     */
    default List<LoginTenantVO> listEnabledByUser(Long userId) {
        return List.of();
    }
}
