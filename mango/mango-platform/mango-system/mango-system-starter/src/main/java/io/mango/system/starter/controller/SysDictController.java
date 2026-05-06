package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.system.api.po.DictTypePo;
import io.mango.system.api.po.DictDataPo;
import io.mango.system.api.vo.DictTypeVO;
import io.mango.system.api.vo.DictDataVO;
import io.mango.system.api.vo.DictOptionVO;
import io.mango.system.core.service.IDictService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final IDictService dictService;

    @GetMapping("/type/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:list")
    public R<List<DictTypeVO>> listTypes() {
        return dictService.listTypes();
    }

    @GetMapping("/type/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:query")
    public R<DictTypeVO> getType(@RequestParam Long id) {
        return dictService.getType(id);
    }

    @PostMapping("/type")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:add")
    public R<Long> createType(@RequestBody @Valid DictTypePo po) {
        return dictService.createType(po);
    }

    @PutMapping("/type")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:edit")
    public R<Boolean> updateType(@RequestBody @Valid DictTypePo po) {
        return dictService.updateType(po);
    }

    @DeleteMapping("/type")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:type:delete")
    public R<Boolean> deleteType(@RequestParam Long id) {
        return dictService.deleteType(id);
    }

    @GetMapping("/data/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:list")
    public R<List<DictDataVO>> listData(@RequestParam(required = false) Long typeId) {
        return dictService.listData(typeId);
    }

    @GetMapping("/data/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:query")
    public R<DictDataVO> getData(@RequestParam Long id) {
        return dictService.getData(id);
    }

    @PostMapping("/data")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:add")
    public R<Long> createData(@RequestBody @Valid DictDataPo po) {
        return dictService.createData(po);
    }

    @PutMapping("/data")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:edit")
    public R<Boolean> updateData(@RequestBody @Valid DictDataPo po) {
        return dictService.updateData(po);
    }

    @DeleteMapping("/data")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:delete")
    public R<Boolean> deleteData(@RequestParam Long id) {
        return dictService.deleteData(id);
    }

    @GetMapping("/data/options")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:dict:data:list")
    public R<List<DictOptionVO>> getOptions(@RequestParam String typeCode) {
        return dictService.getOptions(typeCode);
    }
}
