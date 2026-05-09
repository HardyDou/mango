package io.mango.system.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.system.api.po.SysLoginLogPo;
import io.mango.system.api.po.SysOperationLogPo;
import io.mango.system.api.query.LoginLogPageQuery;
import io.mango.system.api.query.OperationLogPageQuery;

import java.util.List;
import java.util.Map;

public interface ISysLogService {

    R<List<SysLoginLogPo>> listLoginLogs();

    R<PageResult<SysLoginLogPo>> pageLoginLogs(LoginLogPageQuery query);

    R<SysLoginLogPo> getLoginLog(Long id);

    R<Boolean> recordLoginLog(SysLoginLogPo log);

    R<Boolean> cleanLoginLogs(Integer retentionDays);

    R<Map<String, Object>> loginStatistics();

    R<List<SysOperationLogPo>> listOperationLogs();

    R<PageResult<SysOperationLogPo>> pageOperationLogs(OperationLogPageQuery query);

    R<SysOperationLogPo> getOperationLog(Long id);

    R<Boolean> recordOperationLog(SysOperationLogPo log);

    R<Boolean> cleanOperationLogs(Integer retentionDays);
}
