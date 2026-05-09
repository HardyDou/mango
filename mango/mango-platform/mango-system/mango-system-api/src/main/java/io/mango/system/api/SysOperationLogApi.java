package io.mango.system.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.system.api.po.SysOperationLogPo;
import io.mango.system.api.query.OperationLogPageQuery;

import java.util.List;

public interface SysOperationLogApi {

    R<List<SysOperationLogPo>> list();

    R<PageResult<SysOperationLogPo>> page(OperationLogPageQuery query);

    R<SysOperationLogPo> get(Long id);

    R<Boolean> record(SysOperationLogPo log);

    R<Boolean> clean(Integer retentionDays);
}
