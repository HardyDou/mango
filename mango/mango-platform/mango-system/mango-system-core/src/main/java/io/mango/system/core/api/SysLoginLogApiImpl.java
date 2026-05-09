package io.mango.system.core.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.system.api.SysLoginLogApi;
import io.mango.system.api.po.SysLoginLogPo;
import io.mango.system.api.query.LoginLogPageQuery;
import io.mango.system.core.service.ISysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SysLoginLogApiImpl implements SysLoginLogApi {

    private final ISysLogService logService;

    @Override
    public R<List<SysLoginLogPo>> list() {
        return logService.listLoginLogs();
    }

    @Override
    public R<PageResult<SysLoginLogPo>> page(LoginLogPageQuery query) {
        return logService.pageLoginLogs(query);
    }

    @Override
    public R<SysLoginLogPo> get(Long id) {
        return logService.getLoginLog(id);
    }

    @Override
    public R<Boolean> record(SysLoginLogPo log) {
        return logService.recordLoginLog(log);
    }

    @Override
    public R<Boolean> clean(Integer retentionDays) {
        return logService.cleanLoginLogs(retentionDays);
    }

    @Override
    public R<Map<String, Object>> statistics() {
        return logService.loginStatistics();
    }
}
