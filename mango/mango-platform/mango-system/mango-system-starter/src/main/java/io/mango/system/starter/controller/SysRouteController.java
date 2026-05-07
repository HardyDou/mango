package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.system.api.command.UpdateRouteSortCommand;
import io.mango.system.api.po.SysRoutePo;
import io.mango.system.core.service.ISysRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/route")
@RequiredArgsConstructor
@Tag(name = "系统路由", description = "系统路由列表、树、详情、新增、修改与排序接口")
public class SysRouteController {

    private final ISysRouteService routeService;

    @GetMapping("/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:route:list")
    @Operation(summary = "获取系统路由列表", description = "权限接口。查询全部系统路由列表")
    public R<List<SysRoutePo>> list() {
        return routeService.list();
    }

    @GetMapping("/tree")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:route:list")
    @Operation(summary = "获取系统路由树", description = "权限接口。查询系统路由树")
    public R<List<SysRoutePo>> tree() {
        return routeService.tree();
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:route:query")
    @Operation(summary = "获取系统路由详情", description = "权限接口。按路由ID查询系统路由详情")
    public R<SysRoutePo> get(
            @Parameter(description = "路由ID")
            @RequestParam Long id) {
        return routeService.get(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:route:add")
    @Operation(summary = "新增系统路由", description = "权限接口。创建系统路由")
    public R<Long> create(@RequestBody @Valid SysRoutePo po) {
        return routeService.create(po);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:route:edit")
    @Operation(summary = "修改系统路由", description = "权限接口。更新系统路由")
    public R<Boolean> update(@RequestBody @Valid SysRoutePo po) {
        return routeService.update(po);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:route:delete")
    @Operation(summary = "删除系统路由", description = "权限接口。按路由ID删除系统路由")
    public R<Boolean> delete(
            @Parameter(description = "路由ID")
            @RequestParam Long id) {
        return routeService.delete(id);
    }

    @PutMapping("/sort")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:route:edit")
    @Operation(summary = "调整系统路由排序", description = "权限接口。按路由ID顺序批量调整系统路由排序")
    public R<Boolean> updateSort(@RequestBody @Valid UpdateRouteSortCommand command) {
        return routeService.updateSort(command.getIds());
    }
}
