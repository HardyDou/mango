package io.mango.system.core.service;

import io.mango.common.result.R;
import io.mango.system.api.po.SysLoginLogPo;
import io.mango.system.api.po.SysOperationLogPo;

import java.util.List;
import java.util.Map;

public interface ISysLogService {

    R<List<SysLoginLogPo>> listLoginLogs();

    R<SysLoginLogPo> getLoginLog(Long id);

    R<Boolean> cleanLoginLogs();

    R<Map<String, Object>> loginStatistics();

    R<List<SysOperationLogPo>> listOperationLogs();

    R<SysOperationLogPo> getOperationLog(Long id);

    R<Boolean> cleanOperationLogs();
}
