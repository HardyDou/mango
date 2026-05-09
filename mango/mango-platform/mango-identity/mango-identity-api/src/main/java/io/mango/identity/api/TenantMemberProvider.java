package io.mango.identity.api;

import io.mango.identity.api.vo.TenantMemberInfo;

import java.util.List;

/**
 * 机构成员事实 Provider。
 */
public interface TenantMemberProvider {

    /**
     * 查询账号在指定机构下的启用成员身份。
     *
     * @param userId 全局账号 ID
     * @param tenantId 机构 ID
     * @return 启用成员身份，不存在时返回 null
     */
    TenantMemberInfo getEnabledMember(Long userId, Long tenantId);

    /**
     * 查询账号已加入且启用的机构成员身份。
     *
     * @param userId 全局账号 ID
     * @return 成员身份列表
     */
    List<TenantMemberInfo> listEnabledMembers(Long userId);
}
