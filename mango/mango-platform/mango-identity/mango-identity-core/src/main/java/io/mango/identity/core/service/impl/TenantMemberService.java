package io.mango.identity.core.service.impl;

import io.mango.common.result.Require;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.enums.IdentityCode;
import io.mango.identity.api.query.TenantMemberOrgExistsQuery;
import io.mango.identity.api.query.TenantMemberOrgOtherCountQuery;
import io.mango.identity.api.request.ListTenantMembersRequest;
import io.mango.identity.api.vo.TenantMemberOrgRelationVO;
import io.mango.identity.api.vo.TenantMemberVO;
import io.mango.identity.core.service.ITenantMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantMemberService implements ITenantMemberService {

    private final TenantMemberProvider tenantMemberProvider;

    @Override
    public TenantMemberVO getEnabledMember(Long userId, Long tenantId) {
        return tenantMemberProvider.getEnabledMember(userId, tenantId);
    }

    @Override
    public List<TenantMemberVO> listEnabledMembers(Long userId) {
        return tenantMemberProvider.listEnabledMembers(userId);
    }

    @Override
    public TenantMemberVO getMember(Long memberId) {
        return tenantMemberProvider.getMember(memberId);
    }

    @Override
    public List<TenantMemberOrgRelationVO> listOrgRelations(Long tenantId, Long orgId) {
        return tenantMemberProvider.listOrgRelations(tenantId, orgId);
    }

    @Override
    public TenantMemberOrgRelationVO getOrgRelation(Long relationId) {
        return tenantMemberProvider.getOrgRelation(relationId);
    }

    @Override
    public boolean existsOrgRelation(TenantMemberOrgExistsQuery query) {
        return tenantMemberProvider.existsOrgRelation(query.getTenantId(), query.getMemberId(), query.getOrgId());
    }
    @Override
    public boolean addOrgRelation(AddTenantMemberOrgCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "成员组织新增命令不能为空");
        tenantMemberProvider.addOrgRelation(command);
        return true;
    }

    @Override
    public boolean updateOrgRelation(UpdateTenantMemberOrgCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "成员组织修改命令不能为空");
        tenantMemberProvider.updateOrgRelation(command);
        return true;
    }

    @Override
    public boolean removeOrgRelation(Long relationId) {
        Require.notNull(relationId, IdentityCode.VALIDATION_ERROR, "成员组织关系ID不能为空");
        tenantMemberProvider.removeOrgRelation(relationId);
        return true;
    }
    @Override
    public long countOtherOrgRelations(TenantMemberOrgOtherCountQuery query) {
        return tenantMemberProvider.countOtherOrgRelations(
                query.getTenantId(), query.getMemberId(), query.getExcludedRelationId());
    }
    @Override
    public List<TenantMemberVO> listMembers(ListTenantMembersRequest request) {
        Require.notNull(request, IdentityCode.VALIDATION_ERROR, "成员列表请求不能为空");
        return tenantMemberProvider.listMembers(request.getMemberIds());
    }
}
