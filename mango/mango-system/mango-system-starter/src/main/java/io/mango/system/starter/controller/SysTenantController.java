package io.mango.system.starter.controller;

import io.mango.common.annotation.Perm;
import io.mango.common.result.R;
import io.mango.system.api.po.SysTenantPo;
import io.mango.system.core.service.ISysTenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/tenant")
@RequiredArgsConstructor
public class SysTenantController {

    private final ISysTenantService tenantService;

    @GetMapping("/list")
    @Perm("system:tenant:list")
    public R<List<SysTenantPo>> list() {
        return tenantService.list();
    }

    @GetMapping("/{id}")
    @Perm("system:tenant:query")
    public R<SysTenantPo> get(@PathVariable Long id) {
        return tenantService.get(id);
    }

    @PostMapping
    @Perm("system:tenant:add")
    public R<Long> create(@RequestBody @Valid SysTenantPo po) {
        return tenantService.create(po);
    }

    @PutMapping
    @Perm("system:tenant:edit")
    public R<Boolean> update(@RequestBody @Valid SysTenantPo po) {
        return tenantService.update(po);
    }

    @DeleteMapping("/{id}")
    @Perm("system:tenant:delete")
    public R<Boolean> delete(@PathVariable Long id) {
        return tenantService.delete(id);
    }

    @PutMapping("/status/{id}")
    @Perm("system:tenant:edit")
    public R<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return tenantService.updateStatus(id, status);
    }
}
