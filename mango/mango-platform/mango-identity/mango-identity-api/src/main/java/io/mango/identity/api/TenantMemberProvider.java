package io.mango.identity.api;

import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.CreateTenantMemberInOrgCommand;
import io.mango.identity.api.command.RestoreTenantMemberInOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.vo.TenantMemberOrgRelationVO;
import io.mango.identity.api.vo.TenantMemberVO;

import java.util.Collection;
import java.util.List;

/**
 * 机构成员事实 Provider。
 */
public interface TenantMemberProvider {

    /**
     * 在指定组织内原子创建账号、租户成员和组织关系。
     *
     * @param command 创建命令
     * @return 新用户 ID
     */
    Long createMemberInOrg(CreateTenantMemberInOrgCommand command);

    /** Restore a retained member and add only the selected organization relation. */
    Long restoreMemberInOrg(RestoreTenantMemberInOrgCommand command);

    /**
     * 查询账号在指定机构下的启用成员身份。
     *
     * @param userId 全局账号 ID
     * @param tenantId 机构 ID
     * @return 启用成员身份，不存在时返回 null
     */
    TenantMemberVO getEnabledMember(Long userId, Long tenantId);

    /**
     * 查询账号已加入且启用的机构成员身份。
     *
     * @param userId 全局账号 ID
     * @return 成员身份列表
     */
    List<TenantMemberVO> listEnabledMembers(Long userId);

    /**
     * 按成员 ID 查询成员身份。
     *
     * @param memberId 成员 ID
     * @return 成员身份
     */
    TenantMemberVO getMember(Long memberId);

    /**
     * 查询组织成员关系。
     *
     * @param tenantId 租户 ID
     * @param orgId 组织 ID
     * @return 成员组织关系列表
     */
    List<TenantMemberOrgRelationVO> listOrgRelations(Long tenantId, Long orgId);

    /**
     * 查询成员组织关系。
     *
     * @param relationId 关系 ID
     * @return 成员组织关系
     */
    TenantMemberOrgRelationVO getOrgRelation(Long relationId);

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
    List<TenantMemberVO> listMembers(Collection<Long> memberIds);
}
