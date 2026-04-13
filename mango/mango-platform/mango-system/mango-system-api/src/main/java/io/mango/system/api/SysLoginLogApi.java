package io.mango.system.api;

import io.mango.common.result.R;
import io.mango.system.api.po.SysLoginLogPo;

import java.util.List;
import java.util.Map;

public interface SysLoginLogApi {

    R<List<SysLoginLogPo>> list();

    R<SysLoginLogPo> get(Long id);

    R<Boolean> clean();

    R<Map<String, Object>> statistics();
}
