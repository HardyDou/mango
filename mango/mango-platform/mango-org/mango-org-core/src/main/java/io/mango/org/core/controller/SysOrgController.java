package io.mango.org.core.controller;

import io.mango.common.result.R;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.core.service.ISysOrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class SysOrgController {

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
    public R<List<SysOrg>> tree(
            @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId,
            @RequestParam(value = "type", required = false) Integer type) {
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
    @GetMapping("/children/{parentId}")
    public R<List<SysOrg>> children(@PathVariable Long parentId) {
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
    @GetMapping("/{id}")
    public R<SysOrg> getById(@PathVariable Long id) {
        SysOrg org = orgService.getById(id);
        return R.ok(org);
    }
}
