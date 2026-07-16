package io.mango.system.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.system.api.query.LoginLogPageQuery;
import io.mango.system.api.vo.LoginStatisticsVO;
import io.mango.system.api.vo.SysLoginLogVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface SysLoginLogApi {
    R<PageResult<SysLoginLogVO>> page(@Valid LoginLogPageQuery query);
    R<SysLoginLogVO> get(@NotNull Long id);
    R<Boolean> clean(@Max(Integer.MAX_VALUE) Integer retentionDays);
    R<LoginStatisticsVO> statistics();
}
