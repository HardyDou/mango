package io.mango.org.core.controller;

import io.mango.common.result.R;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.api.query.SysOrgTreeQuery;
import io.mango.org.core.service.ISysOrgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "组织架构", description = "组织树、组织详情与下级组织查询接口")
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
    @Operation(summary = "获取组织树", description = "登录接口。按父级组织和组织类型懒加载组织树")
    public R<List<SysOrg>> tree(@ParameterObject SysOrgTreeQuery query) {
        Long parentId = query.getParentId() == null ? 0L : query.getParentId();
        Integer type = query.getType();
        log.info("Org tree request: parentId={}, type={}", parentId, type);
        List<SysOrg> tree = orgService.tree(parentId, type);
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
    @Operation(summary = "获取下级组织", description = "登录接口。按父级组织ID查询直属下级组织列表")
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
    @Operation(summary = "获取组织详情", description = "登录接口。按组织ID查询组织详情")
    public R<SysOrg> getById(
            @Parameter(description = "组织ID")
            @RequestParam Long id) {
        SysOrg org = orgService.getById(id);
        return R.ok(org);
    }
}
