package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.system.api.po.SysConfigPo;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.core.service.ISysConfigService;
import io.mango.infra.log.annotation.Log;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "系统配置列表、详情、新增、修改与删除接口")
public class SysConfigController {

    private final ISysConfigService configService;

    @GetMapping("/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:list")
    @Operation(summary = "获取系统配置列表", description = "权限接口。按配置类型查询系统配置列表")
    public R<List<SysConfigPo>> list(
            @Parameter(description = "配置类型")
            @RequestParam(required = false) ConfigTypeEnum type,
            @Parameter(description = "业务域编码")
            @RequestParam(required = false) String domainCode) {
        return configService.list(type, domainCode);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:query")
    @Operation(summary = "获取系统配置详情", description = "权限接口。按配置ID查询系统配置详情")
    public R<SysConfigPo> get(
            @Parameter(description = "配置ID")
            @RequestParam Long id) {
        return configService.get(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:add")
    @Operation(summary = "新增系统配置", description = "权限接口。创建系统配置")
    @Log("新增系统配置")
    public R<Long> create(@RequestBody @Valid SysConfigPo po) {
        return configService.create(po);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:edit")
    @Operation(summary = "修改系统配置", description = "权限接口。更新系统配置")
    @Log("修改系统配置")
    public R<Boolean> update(@RequestBody @Valid SysConfigPo po) {
        return configService.update(po);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:delete")
    @Operation(summary = "删除系统配置", description = "权限接口。按配置ID删除系统配置")
    @Log("删除系统配置")
    public R<Boolean> delete(
            @Parameter(description = "配置ID")
            @RequestParam Long id) {
        return configService.delete(id);
    }

    @PutMapping("/value")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:edit")
    @Operation(summary = "修改系统配置值", description = "权限接口。按配置ID快速修改配置值")
    @Log("修改系统配置值")
    public R<Boolean> updateValue(
            @Parameter(description = "配置ID")
            @RequestParam Long id,
            @Parameter(description = "配置值")
            @RequestParam String value) {
        return configService.updateValue(id, value);
    }

    @GetMapping("/type")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:list")
    @Operation(summary = "按类型获取系统配置", description = "权限接口。按配置类型查询系统配置列表")
    public R<List<SysConfigPo>> listByType(
            @Parameter(description = "配置类型")
            @RequestParam ConfigTypeEnum type,
            @Parameter(description = "业务域编码")
            @RequestParam(required = false) String domainCode) {
        return configService.list(type, domainCode);
    }

    @GetMapping("/groups")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:list")
    @Operation(summary = "获取系统配置分组", description = "权限接口。查询当前系统支持的配置类型分组")
    public R<List<String>> groups() {
        return configService.listTypes();
    }

    @GetMapping("/value-types")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:list")
    @Operation(summary = "获取系统配置展示类型", description = "权限接口。查询配置面板支持的展示与编辑类型")
    public R<List<String>> valueTypes() {
        return configService.listValueTypes();
    }
}
