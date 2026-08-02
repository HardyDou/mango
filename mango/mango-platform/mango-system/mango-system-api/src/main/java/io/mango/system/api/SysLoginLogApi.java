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

    /**
     * 分页查询当前租户中当前登录账号自己的登录日志。
     *
     * @param query 分页与筛选条件
     * @return 当前账号登录日志分页结果
     */
    R<PageResult<SysLoginLogVO>> pageCurrentUser(@Valid LoginLogPageQuery query);
    R<SysLoginLogVO> get(@NotNull Long id);
    R<Boolean> clean(@Max(Integer.MAX_VALUE) Integer retentionDays);
    R<LoginStatisticsVO> statistics();
}
