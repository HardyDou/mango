package io.mango.identity.starter.remote;

import io.mango.common.result.R;
import io.mango.identity.api.TenantMemberApi;
import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.vo.TenantMemberInfo;
import io.mango.identity.api.vo.TenantMemberOrgRelationInfo;
import org.springframework.cloud.openfeign.FeignClient;
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
    @GetMapping("/tenant-members/enabled")
    R<TenantMemberInfo> getEnabledMember(@RequestParam("userId") Long userId,
            @RequestParam("tenantId") Long tenantId);

    @Override
    @GetMapping("/tenant-members/enabled-list")
    R<List<TenantMemberInfo>> listEnabledMembers(@RequestParam("userId") Long userId);

    @Override
    @GetMapping("/tenant-members/detail")
    R<TenantMemberInfo> getMember(@RequestParam("memberId") Long memberId);

    @Override
    @GetMapping("/tenant-members/org-relations")
    R<List<TenantMemberOrgRelationInfo>> listOrgRelations(@RequestParam("tenantId") Long tenantId,
            @RequestParam("orgId") Long orgId);

    @Override
    @GetMapping("/tenant-members/org-relations/detail")
    R<TenantMemberOrgRelationInfo> getOrgRelation(@RequestParam("relationId") Long relationId);

    @Override
    @GetMapping("/tenant-members/org-relations/exists")
    R<Boolean> existsOrgRelation(@RequestParam("tenantId") Long tenantId,
            @RequestParam("memberId") Long memberId,
            @RequestParam("orgId") Long orgId);

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
    R<Long> countOtherOrgRelations(@RequestParam("tenantId") Long tenantId,
            @RequestParam("memberId") Long memberId,
            @RequestParam(value = "excludedRelationId", required = false) Long excludedRelationId);

    @Override
    @PostMapping("/tenant-members/list")
    R<List<TenantMemberInfo>> listMembers(@RequestBody List<Long> memberIds);
}
