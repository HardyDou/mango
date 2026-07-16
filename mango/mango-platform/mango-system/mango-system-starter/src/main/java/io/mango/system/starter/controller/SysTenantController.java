package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.infra.log.annotation.Log;
import io.mango.system.api.SysTenantApi;
import io.mango.system.api.command.SaveSysTenantCommand;
import io.mango.system.api.vo.LoginTenantOptionVO;
import io.mango.system.api.vo.SysTenantVO;
import io.mango.system.core.service.ISysTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/system/tenant")
@RequiredArgsConstructor
@Tag(name = "机构管理", description = "机构管理与登录机构选项接口")
public class SysTenantController implements SysTenantApi {

    private final ISysTenantService tenantService;

    @Override
    @GetMapping("/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:list")
    @Operation(summary = "获取机构列表", description = "获取机构列表并返回处理结果")
    public R<List<SysTenantVO>> list() {
        return R.ok(tenantService.list());
    }

    @Override
    @GetMapping("/login-options")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "登录机构选项")
    @Operation(summary = "获取登录机构选项", description = "获取登录机构选项并返回处理结果")
    public R<List<LoginTenantOptionVO>> listLoginOptions() {
        return R.ok(tenantService.listLoginOptions());
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:query")
    @Operation(summary = "获取机构详情", description = "获取机构详情并返回处理结果")
    public R<SysTenantVO> get(@Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id) {
        return R.ok(tenantService.get(id));
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:add")
    @Operation(summary = "新增机构", description = "新增机构并返回处理结果")
    @Log("新增机构")
    public R<Long> create(@RequestBody SaveSysTenantCommand command) {
        return R.ok(tenantService.create(command));
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:edit")
    @Operation(summary = "修改机构", description = "修改机构并返回处理结果")
    @Log("修改机构")
    public R<Boolean> update(@RequestBody SaveSysTenantCommand command) {
        return R.ok(tenantService.update(command));
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:delete")
    @Operation(summary = "删除机构", description = "删除机构并返回处理结果")
    @Log("删除机构")
    public R<Boolean> delete(@Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id) {
        return R.ok(tenantService.delete(id));
    }

    @Override
    @PutMapping("/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:tenant:edit")
    @Operation(summary = "修改机构状态", description = "修改机构状态并返回处理结果")
    @Log("修改机构状态")
    public R<Boolean> updateStatus(
            @Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id,
            @Parameter(description = "机构状态", required = true) @RequestParam("status") Integer status) {
        return R.ok(tenantService.updateStatus(id, status));
    }
}
