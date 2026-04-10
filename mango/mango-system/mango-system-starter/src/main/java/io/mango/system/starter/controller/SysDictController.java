package io.mango.system.starter.controller;

import io.mango.infra.security.api.Perm;
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
    @Perm("system:dict:type:list")
    public R<List<DictTypeVO>> listTypes() {
        return dictService.listTypes();
    }

    @GetMapping("/type/{id}")
    @Perm("system:dict:type:query")
    public R<DictTypeVO> getType(@PathVariable Long id) {
        return dictService.getType(id);
    }

    @PostMapping("/type")
    @Perm("system:dict:type:add")
    public R<Long> createType(@RequestBody @Valid DictTypePo po) {
        return dictService.createType(po);
    }

    @PutMapping("/type")
    @Perm("system:dict:type:edit")
    public R<Boolean> updateType(@RequestBody @Valid DictTypePo po) {
        return dictService.updateType(po);
    }

    @DeleteMapping("/type/{id}")
    @Perm("system:dict:type:delete")
    public R<Boolean> deleteType(@PathVariable Long id) {
        return dictService.deleteType(id);
    }

    @GetMapping("/data/list")
    @Perm("system:dict:data:list")
    public R<List<DictDataVO>> listData(@RequestParam(required = false) Long typeId) {
        return dictService.listData(typeId);
    }

    @GetMapping("/data/{id}")
    @Perm("system:dict:data:query")
    public R<DictDataVO> getData(@PathVariable Long id) {
        return dictService.getData(id);
    }

    @PostMapping("/data")
    @Perm("system:dict:data:add")
    public R<Long> createData(@RequestBody @Valid DictDataPo po) {
        return dictService.createData(po);
    }

    @PutMapping("/data")
    @Perm("system:dict:data:edit")
    public R<Boolean> updateData(@RequestBody @Valid DictDataPo po) {
        return dictService.updateData(po);
    }

    @DeleteMapping("/data/{id}")
    @Perm("system:dict:data:delete")
    public R<Boolean> deleteData(@PathVariable Long id) {
        return dictService.deleteData(id);
    }

    @GetMapping("/data/options")
    @Perm("system:dict:data:list")
    public R<List<DictOptionVO>> getOptions(@RequestParam String typeCode) {
        return dictService.getOptions(typeCode);
    }
}
