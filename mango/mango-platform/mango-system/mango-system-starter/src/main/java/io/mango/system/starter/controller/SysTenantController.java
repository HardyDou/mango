package io.mango.system.starter.controller;

import io.mango.auth.api.vo.LoginTenantVO;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.infra.log.annotation.Log;
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
@Tag(name = "机构管理", description = "机构列表、详情、新增、修改、删除与状态管理接口")
public class SysTenantController {

    private final ISysTenantService tenantService;

    @GetMapping("/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:list")
    @Operation(summary = "获取机构列表", description = "权限接口。查询全部机构列表")
    public R<List<SysTenantPo>> list() {
        return tenantService.list();
    }

    @GetMapping("/login-options")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "登录机构选项")
    @Operation(summary = "获取登录机构选项", description = "公开接口。查询启用机构的轻量列表，用于登录页选择机构")
    public R<List<LoginTenantVO>> listLoginOptions() {
        return tenantService.listLoginOptions();
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:query")
    @Operation(summary = "获取机构详情", description = "权限接口。按机构ID查询机构详情")
    public R<SysTenantPo> get(
            @Parameter(description = "机构ID。底层对应 tenantId")
            @RequestParam Long id) {
        return tenantService.get(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:add")
    @Operation(summary = "新增机构", description = "权限接口。创建机构空间")
    @Log("新增机构")
    public R<Long> create(@RequestBody @Valid SysTenantPo po) {
        return tenantService.create(po);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:edit")
    @Operation(summary = "修改机构", description = "权限接口。更新机构信息")
    @Log("修改机构")
    public R<Boolean> update(@RequestBody @Valid SysTenantPo po) {
        return tenantService.update(po);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:delete")
    @Operation(summary = "删除机构", description = "权限接口。按机构ID删除机构")
    @Log("删除机构")
    public R<Boolean> delete(
            @Parameter(description = "机构ID。底层对应 tenantId")
            @RequestParam Long id) {
        return tenantService.delete(id);
    }

    @PutMapping("/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:edit")
    @Operation(summary = "修改机构状态", description = "权限接口。按机构ID启用或停用机构")
    @Log("修改机构状态")
    public R<Boolean> updateStatus(
            @Parameter(description = "机构ID。底层对应 tenantId")
            @RequestParam Long id,
            @Parameter(description = "机构状态：0-禁用，1-启用")
            @RequestParam Integer status) {
        return tenantService.updateStatus(id, status);
    }
}
