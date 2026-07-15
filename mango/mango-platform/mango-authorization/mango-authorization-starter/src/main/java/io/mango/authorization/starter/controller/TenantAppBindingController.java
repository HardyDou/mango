package io.mango.authorization.starter.controller;

import io.mango.authorization.api.TenantAppBindingApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.TenantAppBindingCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.TenantAppBindingQuery;
import io.mango.authorization.api.vo.TenantAppBindingVO;
import io.mango.authorization.core.service.ITenantAppBindingService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
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
@Validated
@Tag(name = "租户前端应用开通", description = "租户可用前端入口开通、停用和查询接口")
public class TenantAppBindingController implements TenantAppBindingApi {

    private final ITenantAppBindingService tenantAppBindingService;

    @Override
    @GetMapping
    @Operation(summary = "查询租户应用开通关系", description = "权限接口。按租户、应用和状态查询开通关系")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:list")
    public R<List<TenantAppBindingVO>> list(@ParameterObject TenantAppBindingQuery query) {
        return R.ok(tenantAppBindingService.list(query));
    }

    @Override
    @PostMapping
    @Operation(summary = "开通或更新租户应用", description = "权限接口。创建或更新租户与授权应用的开通关系")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:edit")
    public R<Long> enable(@RequestBody TenantAppBindingCommand command) {
        return R.ok(tenantAppBindingService.enable(command));
    }

    @Override
    @DeleteMapping
    @Operation(summary = "停用租户应用", description = "权限接口。按租户ID和应用编码停用开通关系")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:edit")
    public R<Boolean> disable(
            @Parameter(description = "租户ID") @RequestParam(name = "tenantId") Long tenantId,
            @Parameter(description = "应用编码") @RequestParam(name = "appCode") String appCode) {
        return R.ok(tenantAppBindingService.disableRequired(tenantId, appCode));
    }
}
