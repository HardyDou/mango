package io.mango.org.core.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.command.CreateOrgCommand;
import io.mango.org.api.command.UpdateOrgCommand;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.api.query.SysOrgTreeQuery;
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
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:org:add")
    @Operation(summary = "新增组织", description = "权限接口。创建当前租户内的组织")
    public R<Long> create(@Valid @RequestBody CreateOrgCommand command) {
        return R.ok(orgService.create(command));
    }

    @PutMapping
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
}
