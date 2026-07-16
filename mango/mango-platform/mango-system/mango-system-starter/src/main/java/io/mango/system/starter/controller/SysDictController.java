package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.infra.log.annotation.Log;
import io.mango.system.api.DictApi;
import io.mango.system.api.command.SaveDictDataCommand;
import io.mango.system.api.command.SaveDictTypeCommand;
import io.mango.system.api.vo.DictDataVO;
import io.mango.system.api.vo.DictOptionVO;
import io.mango.system.api.vo.DictTypeVO;
import io.mango.system.core.service.IDictService;
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
@RequestMapping("/system/dict")
@RequiredArgsConstructor
@Tag(name = "系统字典", description = "字典类型与字典数据管理接口")
public class SysDictController implements DictApi {

    private final IDictService dictService;

    @Override
    @GetMapping("/type/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:list")
    @Operation(summary = "获取字典类型列表", description = "获取字典类型列表并返回处理结果")
    public R<List<DictTypeVO>> listTypes(@Parameter(description = "业务域编码", required = false) @RequestParam(value = "domainCode", required = false) String domainCode) {
        return R.ok(dictService.listTypes(domainCode));
    }

    @Override
    @GetMapping("/type/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:query")
    @Operation(summary = "获取字典类型详情", description = "获取字典类型详情并返回处理结果")
    public R<DictTypeVO> getType(@Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id) {
        return R.ok(dictService.getType(id));
    }

    @Override
    @PostMapping("/type")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:add")
    @Operation(summary = "新增字典类型", description = "新增字典类型并返回处理结果")
    @Log("新增字典类型")
    public R<Long> createType(@RequestBody SaveDictTypeCommand command) {
        return R.ok(dictService.createType(command));
    }

    @Override
    @PutMapping("/type")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:edit")
    @Operation(summary = "修改字典类型", description = "修改字典类型并返回处理结果")
    @Log("修改字典类型")
    public R<Boolean> updateType(@RequestBody SaveDictTypeCommand command) {
        return R.ok(dictService.updateType(command));
    }

    @Override
    @DeleteMapping("/type")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:delete")
    @Operation(summary = "删除字典类型", description = "删除字典类型并返回处理结果")
    @Log("删除字典类型")
    public R<Boolean> deleteType(@Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id) {
        return R.ok(dictService.deleteType(id));
    }

    @Override
    @GetMapping("/data/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:list")
    @Operation(summary = "获取字典数据列表", description = "获取字典数据列表并返回处理结果")
    public R<List<DictDataVO>> listData(@Parameter(description = "字典类型 ID", required = false) @RequestParam(value = "typeId", required = false) Long typeId) {
        return R.ok(dictService.listData(typeId));
    }

    @Override
    @GetMapping("/data/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:query")
    @Operation(summary = "获取字典数据详情", description = "获取字典数据详情并返回处理结果")
    public R<DictDataVO> getData(@Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id) {
        return R.ok(dictService.getData(id));
    }

    @Override
    @PostMapping("/data")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:add")
    @Operation(summary = "新增字典数据", description = "新增字典数据并返回处理结果")
    @Log("新增字典数据")
    public R<Long> createData(@RequestBody SaveDictDataCommand command) {
        return R.ok(dictService.createData(command));
    }

    @Override
    @PutMapping("/data")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:edit")
    @Operation(summary = "修改字典数据", description = "修改字典数据并返回处理结果")
    @Log("修改字典数据")
    public R<Boolean> updateData(@RequestBody SaveDictDataCommand command) {
        return R.ok(dictService.updateData(command));
    }

    @Override
    @DeleteMapping("/data")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:delete")
    @Operation(summary = "删除字典数据", description = "删除字典数据并返回处理结果")
    @Log("删除字典数据")
    public R<Boolean> deleteData(@Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id) {
        return R.ok(dictService.deleteData(id));
    }

    @Override
    @GetMapping("/data/options")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "获取字典选项")
    @Operation(summary = "获取字典选项", description = "获取字典选项并返回处理结果")
    public R<List<DictOptionVO>> getOptions(@Parameter(description = "字典类型编码", required = true) @RequestParam("typeCode") String typeCode) {
        return R.ok(dictService.getOptions(typeCode));
    }
}
