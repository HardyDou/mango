package io.mango.identity.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
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
import io.mango.identity.core.service.ITenantMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

/**
 * 机构成员事实内部接口。
 */
@ApiAccess(mode = ApiResourceAccessMode.INTERNAL)
@RestController
@RequestMapping("/identity")
@RequiredArgsConstructor
@Validated
@Tag(name = "身份成员-内部", description = "机构成员事实内部接口")
public class TenantMemberController implements TenantMemberApi {

    private final ITenantMemberService tenantMemberService;

    @Override
    @PostMapping("/tenant-members/org-accounts")
    @Operation(summary = "在组织内创建成员账号", description = "内部接口。原子创建账号、租户成员和组织关系")
    public R<Long> createMemberInOrg(@RequestBody CreateTenantMemberInOrgCommand command) {
        return R.ok(tenantMemberService.createMemberInOrg(command));
    }

    @Override
    @PostMapping("/tenant-members/restore-in-org")
    @Operation(summary = "恢复原成员到组织", description = "内部接口。恢复保留的原成员并只建立指定组织关系")
    public R<Long> restoreMemberInOrg(@RequestBody RestoreTenantMemberInOrgCommand command) {
        return R.ok(tenantMemberService.restoreMemberInOrg(command));
    }

    @Override
    @GetMapping("/tenant-members/enabled")
    @Operation(summary = "查询账号启用成员身份", description = "内部接口。按用户和机构查询启用成员身份")
    public R<TenantMemberVO> getEnabledMember(
            @Parameter(description = "用户ID") @RequestParam("userId") Long userId,
            @Parameter(description = "租户ID") @RequestParam("tenantId") Long tenantId) {
        return R.ok(tenantMemberService.getEnabledMember(userId, tenantId));
    }

    @Override
    @GetMapping("/tenant-members/enabled-list")
    @Operation(summary = "查询账号启用成员身份列表", description = "内部接口。按用户查询启用成员身份列表")
    public R<List<TenantMemberVO>> listEnabledMembers(
            @Parameter(description = "用户ID") @RequestParam("userId") Long userId) {
        return R.ok(tenantMemberService.listEnabledMembers(userId));
    }

    @Override
    @GetMapping("/tenant-members/detail")
    @Operation(summary = "查询成员身份", description = "内部接口。按成员ID查询成员身份")
    public R<TenantMemberVO> getMember(
            @Parameter(description = "成员ID") @RequestParam("memberId") Long memberId) {
        return R.ok(tenantMemberService.getMember(memberId));
    }

    @Override
    @GetMapping("/tenant-members/org-relations")
    @Operation(summary = "查询组织成员关系", description = "内部接口。按机构和组织查询成员组织关系")
    public R<List<TenantMemberOrgRelationVO>> listOrgRelations(
            @Parameter(description = "租户ID") @RequestParam("tenantId") Long tenantId,
            @Parameter(description = "组织ID") @RequestParam("orgId") Long orgId) {
        return R.ok(tenantMemberService.listOrgRelations(tenantId, orgId));
    }

    @Override
    @GetMapping("/tenant-members/org-relations/detail")
    @Operation(summary = "查询成员组织关系", description = "内部接口。按关系ID查询成员组织关系")
    public R<TenantMemberOrgRelationVO> getOrgRelation(
            @Parameter(description = "关系ID") @RequestParam("relationId") Long relationId) {
        return R.ok(tenantMemberService.getOrgRelation(relationId));
    }

    @Override
    @GetMapping("/tenant-members/org-relations/exists")
    @Operation(summary = "判断成员组织关系是否存在", description = "内部接口。判断成员组织关系是否存在")
    public R<Boolean> existsOrgRelation(@ParameterObject TenantMemberOrgExistsQuery query) {
        return R.ok(tenantMemberService.existsOrgRelation(query));
    }

    @Override
    @PostMapping("/tenant-members/org-relations")
    @Operation(summary = "新增成员组织关系", description = "内部接口。新增成员组织关系")
    public R<Boolean> addOrgRelation(@RequestBody AddTenantMemberOrgCommand command) {
        return R.ok(tenantMemberService.addOrgRelation(command));
    }

    @Override
    @PutMapping("/tenant-members/org-relations")
    @Operation(summary = "更新成员组织关系", description = "内部接口。更新成员组织关系")
    public R<Boolean> updateOrgRelation(@RequestBody UpdateTenantMemberOrgCommand command) {
        return R.ok(tenantMemberService.updateOrgRelation(command));
    }

    @Override
    @DeleteMapping("/tenant-members/org-relations")
    @Operation(summary = "移除成员组织关系", description = "内部接口。移除成员组织关系")
    public R<Boolean> removeOrgRelation(
            @Parameter(description = "关系ID") @RequestParam("relationId") Long relationId) {
        return R.ok(tenantMemberService.removeOrgRelation(relationId));
    }

    @Override
    @GetMapping("/tenant-members/org-relations/other-count")
    @Operation(summary = "查询其它组织关系数量", description = "内部接口。查询成员其它组织关系数量")
    public R<Long> countOtherOrgRelations(@ParameterObject TenantMemberOrgOtherCountQuery query) {
        return R.ok(tenantMemberService.countOtherOrgRelations(query));
    }

    @Override
    @PostMapping("/tenant-members/list")
    @Operation(summary = "批量查询成员身份", description = "内部接口。按成员ID批量查询成员身份")
    public R<List<TenantMemberVO>> listMembers(@RequestBody ListTenantMembersRequest request) {
        return R.ok(tenantMemberService.listMembers(request));
    }
}
