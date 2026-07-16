package io.mango.identity.core.service;

import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.query.TenantMemberOrgExistsQuery;
import io.mango.identity.api.query.TenantMemberOrgOtherCountQuery;
import io.mango.identity.api.request.ListTenantMembersRequest;
import io.mango.identity.api.vo.TenantMemberOrgRelationVO;
import io.mango.identity.api.vo.TenantMemberVO;

import java.util.List;

public interface ITenantMemberService {

    TenantMemberVO getEnabledMember(Long userId, Long tenantId);
    List<TenantMemberVO> listEnabledMembers(Long userId);
    TenantMemberVO getMember(Long memberId);
    List<TenantMemberOrgRelationVO> listOrgRelations(Long tenantId, Long orgId);
    TenantMemberOrgRelationVO getOrgRelation(Long relationId);
    boolean existsOrgRelation(TenantMemberOrgExistsQuery query);
    boolean addOrgRelation(AddTenantMemberOrgCommand command);
    boolean updateOrgRelation(UpdateTenantMemberOrgCommand command);
    boolean removeOrgRelation(Long relationId);
    long countOtherOrgRelations(TenantMemberOrgOtherCountQuery query);
    List<TenantMemberVO> listMembers(ListTenantMembersRequest request);
}
