package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.system.api.po.SysTenantPo;
import io.mango.system.core.service.ISysTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/tenant")
@RequiredArgsConstructor
@Tag(name = "租户管理", description = "租户列表、详情、新增、修改、删除与状态管理接口")
public class SysTenantController {

    private final ISysTenantService tenantService;

    @GetMapping("/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:list")
    @Operation(summary = "获取租户列表", description = "权限接口。查询全部租户列表")
    public R<List<SysTenantPo>> list() {
        return tenantService.list();
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:query")
    @Operation(summary = "获取租户详情", description = "权限接口。按租户ID查询租户详情")
    public R<SysTenantPo> get(
            @Parameter(description = "租户ID")
            @RequestParam Long id) {
        return tenantService.get(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:add")
    @Operation(summary = "新增租户", description = "权限接口。创建租户")
    public R<Long> create(@RequestBody @Valid SysTenantPo po) {
        return tenantService.create(po);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:edit")
    @Operation(summary = "修改租户", description = "权限接口。更新租户信息")
    public R<Boolean> update(@RequestBody @Valid SysTenantPo po) {
        return tenantService.update(po);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:delete")
    @Operation(summary = "删除租户", description = "权限接口。按租户ID删除租户")
    public R<Boolean> delete(
            @Parameter(description = "租户ID")
            @RequestParam Long id) {
        return tenantService.delete(id);
    }

    @PutMapping("/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:edit")
    @Operation(summary = "修改租户状态", description = "权限接口。按租户ID启用或停用租户")
    public R<Boolean> updateStatus(
            @Parameter(description = "租户ID")
            @RequestParam Long id,
            @Parameter(description = "租户状态：0-禁用，1-启用")
            @RequestParam Integer status) {
        return tenantService.updateStatus(id, status);
    }
}
