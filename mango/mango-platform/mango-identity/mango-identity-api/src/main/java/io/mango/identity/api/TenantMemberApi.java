package io.mango.identity.api;

import io.mango.common.result.R;
import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.vo.TenantMemberInfo;
import io.mango.identity.api.vo.TenantMemberOrgRelationInfo;

import java.util.List;

/**
 * 机构成员事实 HTTP 契约。
 */
public interface TenantMemberApi {

    /** 查询账号在指定机构下的启用成员身份。 */
    R<TenantMemberInfo> getEnabledMember(Long userId, Long tenantId);

    /** 查询账号已加入且启用的机构成员身份。 */
    R<List<TenantMemberInfo>> listEnabledMembers(Long userId);

    /** 按成员 ID 查询成员身份。 */
    R<TenantMemberInfo> getMember(Long memberId);

    /** 查询组织成员关系。 */
    R<List<TenantMemberOrgRelationInfo>> listOrgRelations(Long tenantId, Long orgId);

    /** 查询成员组织关系。 */
    R<TenantMemberOrgRelationInfo> getOrgRelation(Long relationId);

    /** 判断成员组织关系是否存在。 */
    R<Boolean> existsOrgRelation(Long tenantId, Long memberId, Long orgId);

    /** 新增成员组织关系。 */
    R<Boolean> addOrgRelation(AddTenantMemberOrgCommand command);

    /** 更新成员组织关系。 */
    R<Boolean> updateOrgRelation(UpdateTenantMemberOrgCommand command);

    /** 移除成员组织关系。 */
    R<Boolean> removeOrgRelation(Long relationId);

    /** 查询成员其它组织关系数量。 */
    R<Long> countOtherOrgRelations(Long tenantId, Long memberId, Long excludedRelationId);

    /** 批量查询成员身份。 */
    R<List<TenantMemberInfo>> listMembers(List<Long> memberIds);
}
