package io.mango.area.core.controller;

import io.mango.area.api.entity.SysArea;
import io.mango.area.core.service.ISysAreaService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * Area controller for administrative divisions
 *
 * @author Mango
 */
@RestController
@RequestMapping("/system/area")
@RequiredArgsConstructor
@Tag(name = "行政区划", description = "行政区划树、详情、子级与启用区划接口")
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
    @Operation(summary = "获取行政区划树", description = "登录接口。按父级行政区划ID懒加载行政区划列表")
    public R<List<SysArea>> tree(
            @Parameter(description = "父级行政区划ID，根节点为 0")
            @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId) {
        // 返回平铺列表，不包含children，支持懒加载
        List<SysArea> areas = areaService.listByPid(parentId);
        return R.ok(areas);
    }

    /**
     * Get area by ID
     */
    @GetMapping("/detail")
    @Operation(summary = "获取行政区划详情", description = "登录接口。按行政区划ID查询详情")
    public R<SysArea> getById(
            @Parameter(description = "行政区划ID")
            @RequestParam Long id) {
        SysArea area = areaService.getById(id);
        if (area == null) {
            return R.fail(404, "Area not found");
        }
        return R.ok(area);
    }

    /**
     * Get area by adcode
     */
    @GetMapping("/adcode")
    @Operation(summary = "按区划编码获取行政区划", description = "登录接口。按行政区划编码查询详情")
    public R<SysArea> getByAdcode(
            @Parameter(description = "行政区划编码")
            @RequestParam Long adcode) {
        SysArea area = areaService.getByAdcode(adcode);
        if (area == null) {
            return R.fail(404, "Area not found");
        }
        return R.ok(area);
    }

    /**
     * Get children by parent ID
     */
    @GetMapping("/children")
    @Operation(summary = "获取下级行政区划", description = "登录接口。按父级行政区划ID查询直属下级列表")
    public R<List<SysArea>> listByPid(
            @Parameter(description = "父级行政区划ID")
            @RequestParam("parentId") Long parentId) {
        return R.ok(areaService.listByPid(parentId));
    }

    /**
     * Get all active areas
     */
    @GetMapping("/active")
    @Operation(summary = "获取启用行政区划", description = "登录接口。查询所有启用状态的行政区划")
    public R<List<SysArea>> listActive() {
        return R.ok(areaService.listActive());
    }

    /**
     * Create area
     */
    @PostMapping
    @Operation(summary = "新增行政区划", description = "登录接口。创建行政区划")
    public R<Void> create(@RequestBody SysArea area) {
        if (areaService.save(area)) {
            return R.ok();
        }
        return R.fail("Failed to create area");
    }

    /**
     * Update area
     */
    @PutMapping
    @Operation(summary = "修改行政区划", description = "登录接口。更新行政区划")
    public R<Void> update(@RequestBody SysArea area) {
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
    @DeleteMapping
    @Operation(summary = "删除行政区划", description = "登录接口。按行政区划ID删除行政区划")
    public R<Void> delete(
            @Parameter(description = "行政区划ID")
            @RequestParam Long id) {
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
