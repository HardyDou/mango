package io.mango.system.core.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.system.api.SysOperationLogApi;
import io.mango.system.api.po.SysOperationLogPo;
import io.mango.system.api.query.OperationLogPageQuery;
import io.mango.system.core.service.ISysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysOperationLogApiImpl implements SysOperationLogApi {

    private final ISysLogService logService;

    @Override
    public R<List<SysOperationLogPo>> list() {
        return logService.listOperationLogs();
    }

    @Override
    public R<PageResult<SysOperationLogPo>> page(OperationLogPageQuery query) {
        return logService.pageOperationLogs(query);
    }

    @Override
    public R<SysOperationLogPo> get(Long id) {
        return logService.getOperationLog(id);
    }

    @Override
    public R<Boolean> record(SysOperationLogPo log) {
        return logService.recordOperationLog(log);
    }

    @Override
    public R<Boolean> clean(Integer retentionDays) {
        return logService.cleanOperationLogs(retentionDays);
    }
}
