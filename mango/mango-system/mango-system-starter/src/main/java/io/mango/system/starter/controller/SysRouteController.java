package io.mango.system.starter.controller;

import io.mango.common.annotation.Perm;
import io.mango.common.result.R;
import io.mango.system.api.po.SysRoutePo;
import io.mango.system.core.service.ISysRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/route")
@RequiredArgsConstructor
public class SysRouteController {

    private final ISysRouteService routeService;

    @GetMapping("/list")
    @Perm("system:route:list")
    public R<List<SysRoutePo>> list() {
        return routeService.list();
    }

    @GetMapping("/tree")
    @Perm("system:route:list")
    public R<List<SysRoutePo>> tree() {
        return routeService.tree();
    }

    @GetMapping("/{id}")
    @Perm("system:route:query")
    public R<SysRoutePo> get(@PathVariable Long id) {
        return routeService.get(id);
    }

    @PostMapping
    @Perm("system:route:add")
    public R<Long> create(@RequestBody @Valid SysRoutePo po) {
        return routeService.create(po);
    }

    @PutMapping
    @Perm("system:route:edit")
    public R<Boolean> update(@RequestBody @Valid SysRoutePo po) {
        return routeService.update(po);
    }

    @DeleteMapping("/{id}")
    @Perm("system:route:delete")
    public R<Boolean> delete(@PathVariable Long id) {
        return routeService.delete(id);
    }

    @PutMapping("/sort")
    @Perm("system:route:edit")
    public R<Boolean> updateSort(@RequestBody List<Long> ids) {
        return routeService.updateSort(ids);
    }
}
