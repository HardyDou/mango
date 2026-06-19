package io.mango.identity.api;

import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.vo.TenantMemberOrgRelationInfo;
import io.mango.identity.api.vo.TenantMemberInfo;

import java.util.Collection;
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

    /**
     * 按成员 ID 查询成员身份。
     *
     * @param memberId 成员 ID
     * @return 成员身份
     */
    TenantMemberInfo getMember(Long memberId);

    /**
     * 查询组织成员关系。
     *
     * @param tenantId 租户 ID
     * @param orgId 组织 ID
     * @return 成员组织关系列表
     */
    List<TenantMemberOrgRelationInfo> listOrgRelations(Long tenantId, Long orgId);

    /**
     * 查询成员组织关系。
     *
     * @param relationId 关系 ID
     * @return 成员组织关系
     */
    TenantMemberOrgRelationInfo getOrgRelation(Long relationId);

    /**
     * 判断成员组织关系是否存在。
     *
     * @param tenantId 租户 ID
     * @param memberId 成员 ID
     * @param orgId 组织 ID
     * @return 是否存在
     */
    boolean existsOrgRelation(Long tenantId, Long memberId, Long orgId);

    /**
     * 新增成员组织关系。
     *
     * @param command 新增命令
     */
    void addOrgRelation(AddTenantMemberOrgCommand command);

    /**
     * 更新成员组织关系。
     *
     * @param command 更新命令
     */
    void updateOrgRelation(UpdateTenantMemberOrgCommand command);

    /**
     * 移除成员组织关系。
     *
     * @param relationId 关系 ID
     */
    void removeOrgRelation(Long relationId);

    /**
     * 查询成员其它组织关系数量。
     *
     * @param tenantId 租户 ID
     * @param memberId 成员 ID
     * @param excludedRelationId 排除关系 ID
     * @return 其它关系数量
     */
    long countOtherOrgRelations(Long tenantId, Long memberId, Long excludedRelationId);

    /**
     * 批量查询成员身份。
     *
     * @param memberIds 成员 ID 集合
     * @return 成员身份列表
     */
    List<TenantMemberInfo> listMembers(Collection<Long> memberIds);
}
