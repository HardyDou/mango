package io.mango.system.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.system.api.query.OperationLogPageQuery;
import io.mango.system.api.vo.SysOperationLogVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface SysOperationLogApi {
    R<PageResult<SysOperationLogVO>> page(@Valid OperationLogPageQuery query);
    R<SysOperationLogVO> get(@NotNull Long id);
    R<Boolean> clean(@Min(1) Integer retentionDays);
}
