package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.infra.log.annotation.Log;
import io.mango.system.api.SysConfigApi;
import io.mango.system.api.command.SaveSysConfigCommand;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.vo.SysConfigVO;
import io.mango.system.core.service.ISysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
@RequestMapping("/system/config")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "系统配置管理接口")
public class SysConfigController implements SysConfigApi {

    private final ObjectProvider<ISysConfigService> configServices;

    @PostConstruct
    void validateRequiredDependencies() {
        configService();
    }

    @Override
    @GetMapping("/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:list")
    @Operation(summary = "获取系统配置列表", description = "获取系统配置列表并返回处理结果")
    public R<List<SysConfigVO>> list(@Parameter(description = "配置或区划类型", required = false) @RequestParam(value = "type", required = false) String type,
                                     @Parameter(description = "业务域编码", required = false) @RequestParam(value = "domainCode", required = false) String domainCode) {
        return R.ok(configService().list(type, domainCode));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:query")
    @Operation(summary = "获取系统配置详情", description = "获取系统配置详情并返回处理结果")
    public R<SysConfigVO> get(@Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id) {
        return R.ok(configService().get(id));
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:add")
    @Operation(summary = "新增系统配置", description = "新增系统配置并返回处理结果")
    @Log("新增系统配置")
    public R<Long> create(@RequestBody SaveSysConfigCommand command) {
        return R.ok(configService().create(command));
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:edit")
    @Operation(summary = "修改系统配置", description = "修改系统配置并返回处理结果")
    @Log("修改系统配置")
    public R<Boolean> update(@RequestBody SaveSysConfigCommand command) {
        return R.ok(configService().update(command));
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:delete")
    @Operation(summary = "删除系统配置", description = "删除系统配置并返回处理结果")
    @Log("删除系统配置")
    public R<Boolean> delete(@Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id) {
        return R.ok(configService().delete(id));
    }

    @Override
    @PutMapping("/value")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:edit")
    @Operation(summary = "修改系统配置值", description = "修改系统配置值并返回处理结果")
    @Log("修改系统配置值")
    public R<Boolean> updateValue(
            @Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id,
            @Parameter(description = "配置值", required = true) @RequestParam("value") String value) {
        return R.ok(configService().updateValue(id, value));
    }

    @Override
    @GetMapping("/type")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:list")
    @Operation(summary = "按类型获取系统配置", description = "按类型获取系统配置并返回处理结果")
    public R<List<SysConfigVO>> listByType(@Parameter(description = "配置或区划类型", required = true) @RequestParam("type") ConfigTypeEnum type,
                                           @Parameter(description = "业务域编码", required = false) @RequestParam(value = "domainCode", required = false) String domainCode) {
        return R.ok(configService().listByType(type, domainCode));
    }

    @Override
    @GetMapping("/groups")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:list")
    @Operation(summary = "获取系统配置分组", description = "获取系统配置分组并返回处理结果")
    public R<List<String>> groups() {
        return R.ok(configService().listTypes());
    }

    @Override
    @GetMapping("/value-types")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:config:list")
    @Operation(summary = "获取系统配置展示类型", description = "获取系统配置展示类型并返回处理结果")
    public R<List<String>> valueTypes() {
        return R.ok(configService().listValueTypes());
    }

    private ISysConfigService configService() {
        return configServices.getObject();
    }
}
