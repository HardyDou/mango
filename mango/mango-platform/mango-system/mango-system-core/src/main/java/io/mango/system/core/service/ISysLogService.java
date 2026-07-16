package io.mango.system.core.service;

import io.mango.common.vo.PageResult;
import io.mango.system.api.command.RecordOperationLogCommand;
import io.mango.system.api.command.RecordLoginLogCommand;
import io.mango.system.api.query.LoginLogPageQuery;
import io.mango.system.api.query.OperationLogPageQuery;
import io.mango.system.api.vo.LoginStatisticsVO;
import io.mango.system.api.vo.SysLoginLogVO;
import io.mango.system.api.vo.SysOperationLogVO;

public interface ISysLogService {
    boolean record(RecordLoginLogCommand command);
    PageResult<SysLoginLogVO> pageLoginLogs(LoginLogPageQuery query);
    SysLoginLogVO getLoginLog(Long id);
    Boolean cleanLoginLogs(Integer retentionDays);
    LoginStatisticsVO loginStatistics();
    PageResult<SysOperationLogVO> pageOperationLogs(OperationLogPageQuery query);
    SysOperationLogVO getOperationLog(Long id);
    boolean recordOperationLog(RecordOperationLogCommand command);
    Boolean cleanOperationLogs(Integer retentionDays);
}
