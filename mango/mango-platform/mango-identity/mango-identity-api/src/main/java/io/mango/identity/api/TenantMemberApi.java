package io.mango.identity.api;

import io.mango.common.result.R;
import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.query.TenantMemberOrgExistsQuery;
import io.mango.identity.api.query.TenantMemberOrgOtherCountQuery;
import io.mango.identity.api.request.ListTenantMembersRequest;
import io.mango.identity.api.vo.TenantMemberVO;
import io.mango.identity.api.vo.TenantMemberOrgRelationVO;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 机构成员事实 HTTP 契约。
 */
public interface TenantMemberApi {

    /** 查询账号在指定机构下的启用成员身份。 */
    R<TenantMemberVO> getEnabledMember(@NotNull Long userId, @NotNull Long tenantId);

    /** 查询账号已加入且启用的机构成员身份。 */
    R<List<TenantMemberVO>> listEnabledMembers(@NotNull Long userId);

    /** 按成员 ID 查询成员身份。 */
    R<TenantMemberVO> getMember(@NotNull Long memberId);

    /** 查询组织成员关系。 */
    R<List<TenantMemberOrgRelationVO>> listOrgRelations(@NotNull Long tenantId, @NotNull Long orgId);

    /** 查询成员组织关系。 */
    R<TenantMemberOrgRelationVO> getOrgRelation(@NotNull Long relationId);

    /** 判断成员组织关系是否存在。 */
    R<Boolean> existsOrgRelation(@Valid TenantMemberOrgExistsQuery query);

    /** 新增成员组织关系。 */
    R<Boolean> addOrgRelation(@Valid AddTenantMemberOrgCommand command);

    /** 更新成员组织关系。 */
    R<Boolean> updateOrgRelation(@Valid UpdateTenantMemberOrgCommand command);

    /** 移除成员组织关系。 */
    R<Boolean> removeOrgRelation(@NotNull Long relationId);

    /** 查询成员其它组织关系数量。 */
    R<Long> countOtherOrgRelations(@Valid TenantMemberOrgOtherCountQuery query);

    /** 批量查询成员身份。 */
    R<List<TenantMemberVO>> listMembers(@Valid ListTenantMembersRequest request);
}
