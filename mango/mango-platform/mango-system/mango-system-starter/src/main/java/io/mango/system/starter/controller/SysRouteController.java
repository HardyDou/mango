package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.system.api.po.SysRoutePo;
import io.mango.system.core.service.ISysRouteService;
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
    public R<List<SysRoutePo>> list() {
        return routeService.list();
    }

    @GetMapping("/tree")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:route:list")
    public R<List<SysRoutePo>> tree() {
        return routeService.tree();
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:route:query")
    public R<SysRoutePo> get(@RequestParam Long id) {
        return routeService.get(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:route:add")
    public R<Long> create(@RequestBody @Valid SysRoutePo po) {
        return routeService.create(po);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:route:edit")
    public R<Boolean> update(@RequestBody @Valid SysRoutePo po) {
        return routeService.update(po);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:route:delete")
    public R<Boolean> delete(@RequestParam Long id) {
        return routeService.delete(id);
    }

    @PutMapping("/sort")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:route:edit")
    public R<Boolean> updateSort(@RequestBody List<Long> ids) {
        return routeService.updateSort(ids);
    }
}
