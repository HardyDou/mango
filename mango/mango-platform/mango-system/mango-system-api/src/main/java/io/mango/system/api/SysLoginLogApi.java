package io.mango.system.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.system.api.po.SysLoginLogPo;
import io.mango.system.api.query.LoginLogPageQuery;

import java.util.List;
import java.util.Map;

public interface SysLoginLogApi {

    R<List<SysLoginLogPo>> list();

    R<PageResult<SysLoginLogPo>> page(LoginLogPageQuery query);

    R<SysLoginLogPo> get(Long id);

    R<Boolean> record(SysLoginLogPo log);

    R<Boolean> clean(Integer retentionDays);

    R<Map<String, Object>> statistics();
}
