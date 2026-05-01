package io.mango.area.core.controller;

import io.mango.area.api.entity.SysArea;
import io.mango.area.core.service.ISysAreaService;
import io.mango.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Area controller for administrative divisions
 *
 * @author Mango
 */
@RestController
@RequestMapping("/area")
@RequiredArgsConstructor
public class SysAreaController {

    private final ISysAreaService areaService;

    /**
     * Get area tree with lazy loading
     * Returns root + first level children on first call
     *
     * @param type     area level filter (1-4), null for all levels
     * @param parentId  parent ID for lazy loading (default 0 for root)
     * @return tree structure
     */
    @GetMapping("/tree")
    public R<List<SysArea>> tree(
            @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId) {
        // 返回平铺列表，不包含children，支持懒加载
        List<SysArea> areas = areaService.listByPid(parentId);
        return R.ok(areas);
    }

    /**
     * Get area by ID
     */
    @GetMapping("/{id}")
    public R<SysArea> getById(@PathVariable Long id) {
        SysArea area = areaService.getById(id);
        if (area == null) {
            return R.fail(404, "Area not found");
        }
        return R.ok(area);
    }

    /**
     * Get area by adcode
     */
    @GetMapping("/adcode/{adcode}")
    public R<SysArea> getByAdcode(@PathVariable Long adcode) {
        SysArea area = areaService.getByAdcode(adcode);
        if (area == null) {
            return R.fail(404, "Area not found");
        }
        return R.ok(area);
    }

    /**
     * Get children by parent ID
     */
    @GetMapping("/children/{parentId}")
    public R<List<SysArea>> listByPid(@PathVariable("parentId") Long parentId) {
        return R.ok(areaService.listByPid(parentId));
    }

    /**
     * Get all active areas
     */
    @GetMapping("/active")
    public R<List<SysArea>> listActive() {
        return R.ok(areaService.listActive());
    }

    /**
     * Create area
     */
    @PostMapping
    public R<Void> create(@RequestBody SysArea area) {
        if (areaService.save(area)) {
            return R.ok();
        }
        return R.fail("Failed to create area");
    }

    /**
     * Update area
     */
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SysArea area) {
        area.setId(id);
        try {
            if (areaService.update(area)) {
                return R.ok();
            }
            return R.fail("Failed to update area");
        } catch (UnsupportedOperationException e) {
            return R.fail(400, e.getMessage());
        }
    }

    /**
     * Delete area
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        try {
            if (areaService.delete(id)) {
                return R.ok();
            }
            return R.fail("Failed to delete area");
        } catch (UnsupportedOperationException e) {
            return R.fail(400, e.getMessage());
        }
    }
}
