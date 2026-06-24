package io.mango.system.core.service;

import io.mango.common.result.R;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.enums.ConfigValueTypeEnum;
import io.mango.system.api.po.SysConfigPo;

import java.util.List;

public interface ISysConfigService {

    R<List<SysConfigPo>> list(ConfigTypeEnum type, String domainCode);

    R<SysConfigPo> get(Long id);

    R<Long> create(SysConfigPo po);

    R<Boolean> update(SysConfigPo po);

    R<Boolean> delete(Long id);

    R<Boolean> updateValue(Long id, String value);

    R<String> getValue(String configKey);

    R<Boolean> getBooleanValue(String configKey, Boolean defaultValue);

    R<Integer> getIntegerValue(String configKey, Integer defaultValue);

    R<List<String>> listTypes();

    R<List<String>> listValueTypes();
}
