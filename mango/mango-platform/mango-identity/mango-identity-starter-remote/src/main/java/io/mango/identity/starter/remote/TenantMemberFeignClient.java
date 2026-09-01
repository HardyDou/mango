package io.mango.identity.starter.remote;

import io.mango.common.result.R;
import io.mango.identity.api.TenantMemberApi;
import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.CreateTenantMemberInOrgCommand;
import io.mango.identity.api.command.RestoreTenantMemberInOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.query.TenantMemberOrgExistsQuery;
import io.mango.identity.api.query.TenantMemberOrgOtherCountQuery;
import io.mango.identity.api.request.ListTenantMembersRequest;
import io.mango.identity.api.vo.TenantMemberVO;
import io.mango.identity.api.vo.TenantMemberOrgRelationVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 机构成员事实远程客户端。
 */
@FeignClient(name = "mango-identity", contextId = "tenantMemberFeignClient", path = "/identity")
public interface TenantMemberFeignClient extends TenantMemberApi {

    @Override
    @PostMapping("/tenant-members/org-accounts")
    R<Long> createMemberInOrg(@RequestBody CreateTenantMemberInOrgCommand command);

    @Override
    @PostMapping("/tenant-members/restore-in-org")
    R<Long> restoreMemberInOrg(@RequestBody RestoreTenantMemberInOrgCommand command);

    @Override
    @GetMapping("/tenant-members/enabled")
    R<TenantMemberVO> getEnabledMember(@RequestParam("userId") Long userId,
            @RequestParam("tenantId") Long tenantId);

    @Override
    @GetMapping("/tenant-members/enabled-list")
    R<List<TenantMemberVO>> listEnabledMembers(@RequestParam("userId") Long userId);

    @Override
    @GetMapping("/tenant-members/detail")
    R<TenantMemberVO> getMember(@RequestParam("memberId") Long memberId);

    @Override
    @GetMapping("/tenant-members/org-relations")
    R<List<TenantMemberOrgRelationVO>> listOrgRelations(@RequestParam("tenantId") Long tenantId,
            @RequestParam("orgId") Long orgId);

    @Override
    @GetMapping("/tenant-members/org-relations/detail")
    R<TenantMemberOrgRelationVO> getOrgRelation(@RequestParam("relationId") Long relationId);

    @Override
    @GetMapping("/tenant-members/org-relations/exists")
    R<Boolean> existsOrgRelation(@SpringQueryMap TenantMemberOrgExistsQuery query);

    @Override
    @PostMapping("/tenant-members/org-relations")
    R<Boolean> addOrgRelation(@RequestBody AddTenantMemberOrgCommand command);

    @Override
    @PutMapping("/tenant-members/org-relations")
    R<Boolean> updateOrgRelation(@RequestBody UpdateTenantMemberOrgCommand command);

    @Override
    @DeleteMapping("/tenant-members/org-relations")
    R<Boolean> removeOrgRelation(@RequestParam("relationId") Long relationId);

    @Override
    @GetMapping("/tenant-members/org-relations/other-count")
    R<Long> countOtherOrgRelations(@SpringQueryMap TenantMemberOrgOtherCountQuery query);

    @Override
    @PostMapping("/tenant-members/list")
    R<List<TenantMemberVO>> listMembers(@RequestBody ListTenantMembersRequest request);
}
