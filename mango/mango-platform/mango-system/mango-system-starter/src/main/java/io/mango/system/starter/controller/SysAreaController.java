package io.mango.system.starter.controller;

import io.mango.area.api.SysAreaApi;
import io.mango.area.api.command.SaveAreaCommand;
import io.mango.area.api.vo.SysAreaTreeNodeVO;
import io.mango.area.api.vo.SysAreaVO;
import io.mango.area.core.service.ISysAreaService;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
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
@RequestMapping("/system/area")
@RequiredArgsConstructor
@Tag(name = "行政区划", description = "行政区划树、详情、子级与启用区划接口")
public class SysAreaController implements SysAreaApi {

    private final ObjectProvider<ISysAreaService> areaServices;

    @PostConstruct
    void validateRequiredDependencies() {
        areaService();
    }

    @Override
    @GetMapping("/tree")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "获取行政区划树")
    @Operation(summary = "获取行政区划树", description = "获取行政区划树并返回处理结果")
    public R<List<SysAreaTreeNodeVO>> tree(@Parameter(description = "配置或区划类型", required = false) @RequestParam(value = "type", required = false) Integer type) {
        return R.ok(areaService().tree(type));
    }

    @Override
    @GetMapping("/children")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "获取下级行政区划")
    @Operation(summary = "获取下级行政区划", description = "获取下级行政区划并返回处理结果")
    public R<List<SysAreaVO>> listByPid(@Parameter(description = "父级行政区划 ID", required = true) @RequestParam("parentId") Long parentId) {
        return R.ok(areaService().listByPid(parentId));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:area:query")
    @Operation(summary = "获取行政区划详情", description = "获取行政区划详情并返回处理结果")
    public R<SysAreaVO> getById(@Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id) {
        return R.ok(areaService().getById(id));
    }

    @Override
    @GetMapping("/adcode")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "按区划编码获取行政区划")
    @Operation(summary = "按区划编码获取行政区划", description = "按区划编码获取行政区划并返回处理结果")
    public R<SysAreaVO> getByAdcode(@Parameter(description = "行政区划编码", required = true) @RequestParam("adcode") Long adcode) {
        return R.ok(areaService().getByAdcode(adcode));
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:area:add")
    @Operation(summary = "新增行政区划", description = "新增行政区划并返回处理结果")
    public R<Void> create(@RequestBody SaveAreaCommand command) {
        return R.ok(areaService().create(command));
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:area:edit")
    @Operation(summary = "修改行政区划", description = "修改行政区划并返回处理结果")
    public R<Void> update(@RequestBody SaveAreaCommand command) {
        return R.ok(areaService().update(command));
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:area:delete")
    @Operation(summary = "删除行政区划", description = "删除行政区划并返回处理结果")
    public R<Void> delete(@Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id) {
        return R.ok(areaService().delete(id));
    }

    @Override
    @GetMapping("/active")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "获取启用行政区划")
    @Operation(summary = "获取启用行政区划", description = "获取启用行政区划并返回处理结果")
    public R<List<SysAreaVO>> listActive() {
        return R.ok(areaService().listActive());
    }

    private ISysAreaService areaService() {
        return areaServices.getObject();
    }
}
