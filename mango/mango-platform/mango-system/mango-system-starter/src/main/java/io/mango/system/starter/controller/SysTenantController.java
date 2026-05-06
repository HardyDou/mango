package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
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
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:list")
    public R<List<SysTenantPo>> list() {
        return tenantService.list();
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:query")
    public R<SysTenantPo> get(@RequestParam Long id) {
        return tenantService.get(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:add")
    public R<Long> create(@RequestBody @Valid SysTenantPo po) {
        return tenantService.create(po);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:edit")
    public R<Boolean> update(@RequestBody @Valid SysTenantPo po) {
        return tenantService.update(po);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:delete")
    public R<Boolean> delete(@RequestParam Long id) {
        return tenantService.delete(id);
    }

    @PutMapping("/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:edit")
    public R<Boolean> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        return tenantService.updateStatus(id, status);
    }
}
