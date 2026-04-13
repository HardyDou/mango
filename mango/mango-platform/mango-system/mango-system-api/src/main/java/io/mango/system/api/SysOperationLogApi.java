package io.mango.system.api;

import io.mango.common.result.R;
import io.mango.system.api.po.SysOperationLogPo;

import java.util.List;

public interface SysOperationLogApi {

    R<List<SysOperationLogPo>> list();

    R<SysOperationLogPo> get(Long id);

    R<Boolean> clean();
}
