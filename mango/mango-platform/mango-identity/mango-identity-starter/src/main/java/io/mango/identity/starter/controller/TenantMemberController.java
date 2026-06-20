package io.mango.identity.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.identity.api.TenantMemberApi;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.vo.TenantMemberInfo;
import io.mango.identity.api.vo.TenantMemberOrgRelationInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 机构成员事实内部接口。
 */
@ApiAccess(mode = ApiResourceAccessMode.INTERNAL)
@RestController
@RequestMapping("/identity")
@RequiredArgsConstructor
@Tag(name = "身份成员-内部", description = "机构成员事实内部接口")
public class TenantMemberController implements TenantMemberApi {

    private final TenantMemberProvider tenantMemberProvider;

    @Override
    @GetMapping("/tenant-members/enabled")
    @Operation(summary = "查询账号启用成员身份", description = "内部接口。按用户和机构查询启用成员身份")
    public R<TenantMemberInfo> getEnabledMember(@RequestParam("userId") Long userId,
            @RequestParam("tenantId") Long tenantId) {
        return R.ok(tenantMemberProvider.getEnabledMember(userId, tenantId));
    }

    @Override
    @GetMapping("/tenant-members/enabled-list")
    @Operation(summary = "查询账号启用成员身份列表", description = "内部接口。按用户查询启用成员身份列表")
    public R<List<TenantMemberInfo>> listEnabledMembers(@RequestParam("userId") Long userId) {
        return R.ok(tenantMemberProvider.listEnabledMembers(userId));
    }

    @Override
    @GetMapping("/tenant-members/detail")
    @Operation(summary = "查询成员身份", description = "内部接口。按成员ID查询成员身份")
    public R<TenantMemberInfo> getMember(@RequestParam("memberId") Long memberId) {
        return R.ok(tenantMemberProvider.getMember(memberId));
    }

    @Override
    @GetMapping("/tenant-members/org-relations")
    @Operation(summary = "查询组织成员关系", description = "内部接口。按机构和组织查询成员组织关系")
    public R<List<TenantMemberOrgRelationInfo>> listOrgRelations(@RequestParam("tenantId") Long tenantId,
            @RequestParam("orgId") Long orgId) {
        return R.ok(tenantMemberProvider.listOrgRelations(tenantId, orgId));
    }

    @Override
    @GetMapping("/tenant-members/org-relations/detail")
    @Operation(summary = "查询成员组织关系", description = "内部接口。按关系ID查询成员组织关系")
    public R<TenantMemberOrgRelationInfo> getOrgRelation(@RequestParam("relationId") Long relationId) {
        return R.ok(tenantMemberProvider.getOrgRelation(relationId));
    }

    @Override
    @GetMapping("/tenant-members/org-relations/exists")
    @Operation(summary = "判断成员组织关系是否存在", description = "内部接口。判断成员组织关系是否存在")
    public R<Boolean> existsOrgRelation(@RequestParam("tenantId") Long tenantId,
            @RequestParam("memberId") Long memberId,
            @RequestParam("orgId") Long orgId) {
        return R.ok(tenantMemberProvider.existsOrgRelation(tenantId, memberId, orgId));
    }

    @Override
    @PostMapping("/tenant-members/org-relations")
    @Operation(summary = "新增成员组织关系", description = "内部接口。新增成员组织关系")
    public R<Boolean> addOrgRelation(@Valid @RequestBody AddTenantMemberOrgCommand command) {
        tenantMemberProvider.addOrgRelation(command);
        return R.ok(true);
    }

    @Override
    @PutMapping("/tenant-members/org-relations")
    @Operation(summary = "更新成员组织关系", description = "内部接口。更新成员组织关系")
    public R<Boolean> updateOrgRelation(@Valid @RequestBody UpdateTenantMemberOrgCommand command) {
        tenantMemberProvider.updateOrgRelation(command);
        return R.ok(true);
    }

    @Override
    @DeleteMapping("/tenant-members/org-relations")
    @Operation(summary = "移除成员组织关系", description = "内部接口。移除成员组织关系")
    public R<Boolean> removeOrgRelation(@RequestParam("relationId") Long relationId) {
        tenantMemberProvider.removeOrgRelation(relationId);
        return R.ok(true);
    }

    @Override
    @GetMapping("/tenant-members/org-relations/other-count")
    @Operation(summary = "查询其它组织关系数量", description = "内部接口。查询成员其它组织关系数量")
    public R<Long> countOtherOrgRelations(@RequestParam("tenantId") Long tenantId,
            @RequestParam("memberId") Long memberId,
            @RequestParam(value = "excludedRelationId", required = false) Long excludedRelationId) {
        return R.ok(tenantMemberProvider.countOtherOrgRelations(tenantId, memberId, excludedRelationId));
    }

    @Override
    @PostMapping("/tenant-members/list")
    @Operation(summary = "批量查询成员身份", description = "内部接口。按成员ID批量查询成员身份")
    public R<List<TenantMemberInfo>> listMembers(@RequestBody List<Long> memberIds) {
        return R.ok(tenantMemberProvider.listMembers(memberIds));
    }
}
