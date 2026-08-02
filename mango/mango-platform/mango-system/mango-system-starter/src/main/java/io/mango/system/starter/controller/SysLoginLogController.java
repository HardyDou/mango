package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.system.api.SysLoginLogApi;
import io.mango.system.api.query.LoginLogPageQuery;
import io.mango.system.api.vo.LoginStatisticsVO;
import io.mango.system.api.vo.SysLoginLogVO;
import io.mango.system.core.service.ISysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
@Tag(name = "系统登录日志", description = "登录日志查询、统计与清理接口")
public class SysLoginLogController implements SysLoginLogApi {

    private final ISysLogService logService;

    @Override
    @GetMapping("/login/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:list")
    @Operation(summary = "分页查询登录日志", description = "分页查询登录日志并返回处理结果")
    public R<PageResult<SysLoginLogVO>> page(@ParameterObject LoginLogPageQuery query) {
        return R.ok(logService.pageLoginLogs(query));
    }

    @Override
    @GetMapping("/login/my/list")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询当前账号登录日志")
    @Operation(summary = "分页查询当前账号登录日志", description = "仅返回当前租户中当前登录账号自己的登录记录")
    public R<PageResult<SysLoginLogVO>> pageCurrentUser(@ParameterObject LoginLogPageQuery query) {
        return R.ok(logService.pageCurrentUserLoginLogs(query));
    }

    @Override
    @GetMapping("/login/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:query")
    @Operation(summary = "获取登录日志详情", description = "获取登录日志详情并返回处理结果")
    public R<SysLoginLogVO> get(@Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id) {
        return R.ok(logService.getLoginLog(id));
    }

    @Override
    @DeleteMapping("/login/clean")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:delete")
    @Operation(summary = "清理登录日志", description = "清理登录日志并返回处理结果")
    public R<Boolean> clean(@Parameter(description = "日志保留天数", required = false) @RequestParam(value = "retentionDays", required = false) Integer retentionDays) {
        return R.ok(logService.cleanLoginLogs(retentionDays));
    }

    @Override
    @GetMapping("/login/statistics")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:query")
    @Operation(summary = "获取登录统计", description = "获取登录统计并返回处理结果")
    public R<LoginStatisticsVO> statistics() {
        return R.ok(logService.loginStatistics());
    }
}
