package io.mango.org.core.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateOrgCommand;
import io.mango.org.api.command.UpdateOrgMemberCommand;
import io.mango.org.api.command.UpdateOrgCommand;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.api.query.SysOrgTreeQuery;
import io.mango.org.api.vo.OrgMemberVO;
import io.mango.org.core.service.ISysOrgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Organization controller
 * <p>
 * Provides organization tree API with tenant isolation.
 *
 * @author Mango
 */
@Slf4j
@RestController
@RequestMapping("/org")
@RequiredArgsConstructor
@Tag(name = "组织架构", description = "组织树、组织详情、下级组织、新增、修改与删除接口")
public class SysOrgController implements SysOrgApi {

    private final ISysOrgService orgService;

    /**
     * Get organization tree (lazy loading)
     * <p>
     * Headers:
     * - TENANT-ID: tenant identifier for isolation
     *
     * @param parentId parent organization ID (0 for root, defaults to 0)
     * @param type organization type filter (optional): 1-集团, 2-公司, 3-部门, 4-小组
     * @return tree structure
     */
    @GetMapping("/tree")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:list")
    @Operation(summary = "获取组织树", description = "权限接口。按父级组织和组织类型懒加载组织树")
    public R<List<SysOrg>> tree(@ParameterObject SysOrgTreeQuery query) {
        Long parentId = query.getParentId() == null ? 0L : query.getParentId();
        Integer type = query.getType();
        log.info("Org tree request: parentId={}, type={}", parentId, type);
        List<SysOrg> tree = Boolean.TRUE.equals(query.getIncludeDisabled())
                ? orgService.tree(parentId, type, true)
                : orgService.tree(parentId, type);
        return R.ok(tree);
    }

    /**
     * Get organization children by parent ID
     *
     * @param parentId parent organization ID
     * @return children list
     */
    @GetMapping("/children")
    @Override
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:list")
    @Operation(summary = "获取下级组织", description = "权限接口。按父级组织ID查询直属下级组织列表")
    public R<List<SysOrg>> children(
            @Parameter(description = "父级组织ID")
            @RequestParam Long parentId) {
        log.info("Org children request: parentId={}", parentId);
        List<SysOrg> children = orgService.children(parentId);
        return R.ok(children);
    }

    /**
     * Get organization by ID
     *
     * @param id organization ID
     * @return organization
     */
    @GetMapping("/detail")
    @Override
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:query")
    @Operation(summary = "获取组织详情", description = "权限接口。按组织ID查询组织详情")
    public R<SysOrg> getById(
            @Parameter(description = "组织ID")
            @RequestParam Long id) {
        SysOrg org = orgService.getById(id);
        return R.ok(org);
    }

    @PostMapping
    @Override
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:add")
    @Operation(summary = "新增组织", description = "权限接口。创建当前租户内的组织")
    public R<Long> create(@Valid @RequestBody CreateOrgCommand command) {
        return R.ok(orgService.create(command));
    }

    @PutMapping
    @Override
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:edit")
    @Operation(summary = "修改组织", description = "权限接口。更新当前租户内的组织")
    public R<Void> update(@Valid @RequestBody UpdateOrgCommand command) {
        orgService.update(command);
        return R.ok();
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:delete")
    @Operation(summary = "删除组织", description = "权限接口。按组织ID删除当前租户内的组织")
    public R<Void> delete(
            @Parameter(description = "组织ID")
            @RequestParam Long id) {
        orgService.delete(id);
        return R.ok();
    }

    @GetMapping("/{orgId}/members")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:list")
    @Operation(summary = "获取组织成员", description = "权限接口。查询组织成员、岗位、主组织与部门负责人信息")
    public R<List<OrgMemberVO>> members(
            @Parameter(description = "组织ID")
            @PathVariable Long orgId) {
        return R.ok(orgService.members(orgId));
    }

    @PostMapping("/{orgId}/members")
    @Override
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:edit")
    @Operation(summary = "添加组织成员", description = "权限接口。将机构成员加入组织并设置岗位")
    public R<Void> addMember(
            @Parameter(description = "组织ID")
            @PathVariable Long orgId,
            @Valid @RequestBody AddOrgMemberCommand command) {
        orgService.addMember(orgId, command);
        return R.ok();
    }

    @PutMapping("/members")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:edit")
    @Operation(summary = "修改组织成员关系", description = "权限接口。调整组织成员岗位或主组织")
    public R<Void> updateMember(@Valid @RequestBody UpdateOrgMemberCommand command) {
        orgService.updateMember(command);
        return R.ok();
    }

    @DeleteMapping("/members")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:edit")
    @Operation(summary = "移除组织成员", description = "权限接口。从组织中移除成员关系")
    public R<Void> removeMember(
            @Parameter(description = "组织成员关系ID")
            @RequestParam Long relationId) {
        orgService.removeMember(relationId);
        return R.ok();
    }

    @GetMapping("/leader/{orgId}")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "获取组织负责人", description = "登录接口。按组织ID查询持有部门负责人岗位的成员用户ID")
    public R<List<Long>> leaderUserIds(
            @Parameter(description = "组织ID")
            @PathVariable Long orgId) {
        return R.ok(orgService.leaderUserIds(orgId));
    }
}
