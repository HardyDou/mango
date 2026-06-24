package io.mango.system.core.api;

import io.mango.common.result.R;
import io.mango.system.api.SysConfigApi;
import io.mango.system.api.command.UpdateConfigValueCommand;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.po.SysConfigPo;
import io.mango.system.core.service.ISysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysConfigApiImpl implements SysConfigApi {

    private final ISysConfigService configService;

    @Override
    public R<List<SysConfigPo>> list(ConfigTypeEnum type) {
        return configService.list(type, null);
    }

    @Override
    public R<SysConfigPo> get(Long id) {
        return configService.get(id);
    }

    @Override
    public R<Long> create(SysConfigPo po) {
        return configService.create(po);
    }

    @Override
    public R<Boolean> update(SysConfigPo po) {
        return configService.update(po);
    }

    @Override
    public R<Boolean> delete(Long id) {
        return configService.delete(id);
    }

    @Override
    public R<Boolean> updateValue(UpdateConfigValueCommand command) {
        return configService.updateValue(command.getId(), command.getValue());
    }

    @Override
    public R<String> getValue(String configKey) {
        return configService.getValue(configKey);
    }

    @Override
    public R<Boolean> getBooleanValue(String configKey, Boolean defaultValue) {
        return configService.getBooleanValue(configKey, defaultValue);
    }

    @Override
    public R<Integer> getIntegerValue(String configKey, Integer defaultValue) {
        return configService.getIntegerValue(configKey, defaultValue);
    }
}
