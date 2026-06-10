package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.infra.log.annotation.Log;
import io.mango.system.api.po.DictTypePo;
import io.mango.system.api.po.DictDataPo;
import io.mango.system.api.vo.DictTypeVO;
import io.mango.system.api.vo.DictDataVO;
import io.mango.system.api.vo.DictOptionVO;
import io.mango.system.core.service.IDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
@Tag(name = "系统字典", description = "字典类型与字典数据管理接口")
public class SysDictController {

    private final IDictService dictService;

    @GetMapping("/type/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:list")
    @Operation(summary = "获取字典类型列表", description = "权限接口。查询全部字典类型")
    public R<List<DictTypeVO>> listTypes(
            @Parameter(description = "业务域编码")
            @RequestParam(required = false) String domainCode) {
        return dictService.listTypes(domainCode);
    }

    @GetMapping("/type/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:query")
    @Operation(summary = "获取字典类型详情", description = "权限接口。按字典类型ID查询详情")
    public R<DictTypeVO> getType(
            @Parameter(description = "字典类型ID")
            @RequestParam Long id) {
        return dictService.getType(id);
    }

    @PostMapping("/type")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:add")
    @Operation(summary = "新增字典类型", description = "权限接口。创建字典类型")
    @Log("新增字典类型")
    public R<Long> createType(@RequestBody @Valid DictTypePo po) {
        return dictService.createType(po);
    }

    @PutMapping("/type")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:edit")
    @Operation(summary = "修改字典类型", description = "权限接口。更新字典类型")
    @Log("修改字典类型")
    public R<Boolean> updateType(@RequestBody @Valid DictTypePo po) {
        return dictService.updateType(po);
    }

    @DeleteMapping("/type")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:delete")
    @Operation(summary = "删除字典类型", description = "权限接口。按字典类型ID删除字典类型")
    @Log("删除字典类型")
    public R<Boolean> deleteType(
            @Parameter(description = "字典类型ID")
            @RequestParam Long id) {
        return dictService.deleteType(id);
    }

    @GetMapping("/data/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:list")
    @Operation(summary = "获取字典数据列表", description = "权限接口。按字典类型ID查询字典数据列表")
    public R<List<DictDataVO>> listData(
            @Parameter(description = "字典类型ID")
            @RequestParam(required = false) Long typeId) {
        return dictService.listData(typeId);
    }

    @GetMapping("/data/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:query")
    @Operation(summary = "获取字典数据详情", description = "权限接口。按字典数据ID查询详情")
    public R<DictDataVO> getData(
            @Parameter(description = "字典数据ID")
            @RequestParam Long id) {
        return dictService.getData(id);
    }

    @PostMapping("/data")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:add")
    @Operation(summary = "新增字典数据", description = "权限接口。创建字典数据")
    @Log("新增字典数据")
    public R<Long> createData(@RequestBody @Valid DictDataPo po) {
        return dictService.createData(po);
    }

    @PutMapping("/data")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:edit")
    @Operation(summary = "修改字典数据", description = "权限接口。更新字典数据")
    @Log("修改字典数据")
    public R<Boolean> updateData(@RequestBody @Valid DictDataPo po) {
        return dictService.updateData(po);
    }

    @DeleteMapping("/data")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:delete")
    @Operation(summary = "删除字典数据", description = "权限接口。按字典数据ID删除字典数据")
    @Log("删除字典数据")
    public R<Boolean> deleteData(
            @Parameter(description = "字典数据ID")
            @RequestParam Long id) {
        return dictService.deleteData(id);
    }

    @GetMapping("/data/options")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "获取字典选项")
    @Operation(summary = "获取字典选项", description = "登录接口。按字典类型编码查询可选项，用于前端表单、筛选项和字典标签展示")
    public R<List<DictOptionVO>> getOptions(
            @Parameter(description = "字典类型编码")
            @RequestParam String typeCode) {
        return dictService.getOptions(typeCode);
    }
}
