package io.mango.org.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateSysOrgCommand;
import io.mango.org.api.command.CreateOrgMemberAccountCommand;
import io.mango.org.api.command.UpdateSysOrgCommand;
import io.mango.org.api.command.UpdateOrgMemberCommand;
import io.mango.org.api.query.SysOrgTreeQuery;
import io.mango.org.api.vo.OrgMemberVO;
import io.mango.org.api.vo.SysOrgVO;
import io.mango.org.core.service.ISysOrgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 组织管理 HTTP 适配器。
 */
@RestController
@RequestMapping("/org")
@RequiredArgsConstructor
@Validated
@Tag(name = "组织管理", description = "组织树、组织维护、成员关系与负责人查询接口")
public class SysOrgController implements SysOrgApi {

    private final ISysOrgService orgService;

    @Override
    @GetMapping("/tree")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:list")
    @Operation(summary = "获取组织树", description = "按父级、组织类型和启用状态查询组织树")
    public R<List<SysOrgVO>> tree(@ParameterObject SysOrgTreeQuery query) {
        return R.ok(orgService.tree(query));
    }

    @Override
    @GetMapping("/children")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:list")
    @Operation(summary = "获取下级组织", description = "按父级组织ID查询直属下级组织列表")
    public R<List<SysOrgVO>> children(
            @Parameter(description = "父级组织ID", required = true)
            @RequestParam("parentId") Long parentId) {
        return R.ok(orgService.children(parentId));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:query")
    @Operation(summary = "获取组织详情", description = "按组织ID查询组织详情")
    public R<SysOrgVO> getById(
            @Parameter(description = "组织ID", required = true)
            @RequestParam("id") Long id) {
        return R.ok(orgService.detail(id));
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:add")
    @Operation(summary = "新增组织", description = "在当前租户内创建组织")
    public R<Long> create(@RequestBody CreateSysOrgCommand command) {
        return R.ok(orgService.create(command));
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:edit")
    @Operation(summary = "修改组织", description = "更新当前租户内的组织")
    public R<Boolean> update(@RequestBody UpdateSysOrgCommand command) {
        return R.ok(orgService.update(command));
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:delete")
    @Operation(summary = "删除组织", description = "删除没有下级节点的非根组织")
    public R<Boolean> delete(
            @Parameter(description = "组织ID", required = true)
            @RequestParam("id") Long id) {
        DeleteCommand command = new DeleteCommand();
        command.setId(id);
        return R.ok(orgService.delete(command));
    }

    @Override
    @GetMapping("/members")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:list")
    @Operation(summary = "获取组织成员", description = "查询组织成员、岗位、主组织与负责人信息")
    public R<List<OrgMemberVO>> members(
            @Parameter(description = "组织ID", required = true)
            @RequestParam("orgId") Long orgId) {
        return R.ok(orgService.members(orgId));
    }

    @Override
    @GetMapping("/member-scope")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:list")
    @Operation(summary = "获取组织成员范围", description = "返回当前租户指定启用组织及全部启用下级组织ID")
    public R<List<Long>> memberScope(
            @Parameter(description = "组织ID", required = true)
            @RequestParam("orgId") Long orgId) {
        return R.ok(orgService.memberScope(orgId));
    }

    @Override
    @PostMapping("/member-accounts")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:user:add")
    @Operation(summary = "在组织内新增成员账号", description = "校验组织后原子创建账号、租户成员和组织关系")
    public R<Long> createMemberAccount(@RequestBody CreateOrgMemberAccountCommand command) {
        return R.ok(orgService.createMemberAccount(command));
    }

    @Override
    @PostMapping("/members")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:edit")
    @Operation(summary = "添加组织成员", description = "将机构成员加入组织并设置岗位")
    public R<Boolean> addMember(@RequestBody AddOrgMemberCommand command) {
        return R.ok(orgService.addMember(command));
    }

    @Override
    @PutMapping("/members")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:edit")
    @Operation(summary = "修改组织成员关系", description = "调整组织成员岗位、主组织或负责人标志")
    public R<Boolean> updateMember(@RequestBody UpdateOrgMemberCommand command) {
        return R.ok(orgService.updateMember(command));
    }

    @Override
    @DeleteMapping("/members")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:edit")
    @Operation(summary = "移除组织成员", description = "从组织中移除成员关系")
    public R<Boolean> removeMember(
            @Parameter(description = "组织成员关系ID", required = true)
            @RequestParam("relationId") Long relationId) {
        return R.ok(orgService.removeMember(relationId));
    }

    @Override
    @GetMapping("/leader")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "获取组织负责人", description = "按组织ID查询负责人用户ID")
    public R<List<Long>> leaderUserIds(
            @Parameter(description = "组织ID", required = true)
            @RequestParam("orgId") Long orgId) {
        return R.ok(orgService.leaderUserIds(orgId));
    }
}
