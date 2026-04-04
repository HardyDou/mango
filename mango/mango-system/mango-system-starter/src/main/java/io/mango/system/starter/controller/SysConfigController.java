package io.mango.system.starter.controller;

import io.mango.common.annotation.Perm;
import io.mango.common.result.R;
import io.mango.system.api.po.SysConfigPo;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.core.service.ISysConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final ISysConfigService configService;

    @GetMapping("/list")
    @Perm("system:config:list")
    public R<List<SysConfigPo>> list(@RequestParam(required = false) ConfigTypeEnum type) {
        return configService.list(type);
    }

    @GetMapping("/{id}")
    @Perm("system:config:query")
    public R<SysConfigPo> get(@PathVariable Long id) {
        return configService.get(id);
    }

    @PostMapping
    @Perm("system:config:add")
    public R<Long> create(@RequestBody @Valid SysConfigPo po) {
        return configService.create(po);
    }

    @PutMapping
    @Perm("system:config:edit")
    public R<Boolean> update(@RequestBody @Valid SysConfigPo po) {
        return configService.update(po);
    }

    @DeleteMapping("/{id}")
    @Perm("system:config:delete")
    public R<Boolean> delete(@PathVariable Long id) {
        return configService.delete(id);
    }

    @PutMapping("/value/{id}")
    @Perm("system:config:edit")
    public R<Boolean> updateValue(@PathVariable Long id, @RequestParam String value) {
        return configService.updateValue(id, value);
    }

    @GetMapping("/type/{type}")
    @Perm("system:config:list")
    public R<List<SysConfigPo>> listByType(@PathVariable ConfigTypeEnum type) {
        return configService.list(type);
    }
}
