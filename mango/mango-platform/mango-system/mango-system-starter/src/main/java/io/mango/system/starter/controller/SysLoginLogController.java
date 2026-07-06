package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.system.api.SysLoginLogApi;
import io.mango.system.api.po.SysLoginLogPo;
import io.mango.system.api.query.LoginLogPageQuery;
import io.mango.system.core.service.ISysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
@Tag(name = "系统登录日志", description = "登录日志查询、统计、清理接口")
public class SysLoginLogController implements SysLoginLogApi {

    private final ISysLogService logService;

    @Override
    public R<List<SysLoginLogPo>> list() {
        return logService.listLoginLogs();
    }

    @Override
    @GetMapping("/login/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:list")
    @Operation(summary = "分页查询登录日志", description = "权限接口。按关键字、状态和时间范围分页查询登录日志")
    public R<PageResult<SysLoginLogPo>> page(@ParameterObject LoginLogPageQuery query) {
        return logService.pageLoginLogs(query);
    }

    @Override
    @GetMapping("/login/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:query")
    @Operation(summary = "获取登录日志详情", description = "权限接口。按登录日志ID查询详情")
    public R<SysLoginLogPo> get(
            @Parameter(description = "登录日志ID")
            @RequestParam Long id) {
        return logService.getLoginLog(id);
    }

    @Override
    public R<Boolean> record(SysLoginLogPo log) {
        return logService.recordLoginLog(log);
    }

    @Override
    @DeleteMapping("/login/clean")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:delete")
    @Operation(summary = "清理登录日志", description = "权限接口。按保留天数清理登录日志；不传保留天数时清理当前可见范围内全部登录日志")
    public R<Boolean> clean(
            @Parameter(description = "保留天数。只删除早于该天数的日志；不传或小于等于0表示清理全部")
            @RequestParam(required = false) Integer retentionDays) {
        return logService.cleanLoginLogs(retentionDays);
    }

    @Override
    @GetMapping("/login/statistics")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:query")
    @Operation(summary = "获取登录统计", description = "权限接口。查询登录日志统计数据")
    public R<Map<String, Object>> statistics() {
        return logService.loginStatistics();
    }
}
