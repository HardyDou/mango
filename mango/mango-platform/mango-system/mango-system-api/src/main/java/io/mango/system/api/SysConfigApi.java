package io.mango.system.api;

import io.mango.common.result.R;
import io.mango.system.api.command.UpdateConfigValueCommand;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.po.SysConfigPo;

import java.util.List;

public interface SysConfigApi {

    R<List<SysConfigPo>> list(ConfigTypeEnum type);

    R<SysConfigPo> get(Long id);

    R<Long> create(SysConfigPo po);

    R<Boolean> update(SysConfigPo po);

    R<Boolean> delete(Long id);

    R<Boolean> updateValue(UpdateConfigValueCommand command);

    R<String> getValue(String configKey);

    R<Boolean> getBooleanValue(String configKey, Boolean defaultValue);

    R<Integer> getIntegerValue(String configKey, Integer defaultValue);
}
