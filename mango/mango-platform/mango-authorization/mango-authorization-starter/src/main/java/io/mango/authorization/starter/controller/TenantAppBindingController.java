package io.mango.authorization.starter.controller;

import io.mango.authorization.api.TenantAppBindingApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.TenantAppBindingCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.TenantAppBindingVO;
import io.mango.authorization.core.service.ITenantAppBindingService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 租户前端应用开通控制器。
 */
@RestController
@RequestMapping("/authorization/tenant-app-bindings")
@RequiredArgsConstructor
@Tag(name = "租户前端应用开通", description = "租户可用前端入口开通、停用和查询接口")
public class TenantAppBindingController implements TenantAppBindingApi {

    private final ITenantAppBindingService tenantAppBindingService;

    @Override
    @GetMapping
    @Operation(summary = "查询租户应用开通关系")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:list")
    public R<List<TenantAppBindingVO>> list(
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "应用编码") @RequestParam(required = false) String appCode,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return R.ok(tenantAppBindingService.list(tenantId, appCode, status));
    }

    @Override
    @PostMapping
    @Operation(summary = "开通或更新租户应用")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:edit")
    public R<Long> enable(@Valid @RequestBody TenantAppBindingCommand command) {
        return R.ok(tenantAppBindingService.enable(command));
    }

    @Override
    @DeleteMapping
    @Operation(summary = "停用租户应用")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:edit")
    public R<Boolean> disable(
            @Parameter(description = "租户ID") @RequestParam Long tenantId,
            @Parameter(description = "应用编码") @RequestParam String appCode) {
        Boolean success = tenantAppBindingService.disable(tenantId, appCode);
        return Boolean.TRUE.equals(success) ? R.ok(true) : R.fail(404, "租户应用开通关系不存在");
    }
}
