package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.system.api.po.SysLoginLogPo;
import io.mango.system.api.po.SysOperationLogPo;
import io.mango.system.core.service.ISysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
@Tag(name = "系统日志", description = "登录日志与操作日志查询、统计、清理接口")
public class SysLogController {

    private final ISysLogService logService;

    @GetMapping("/login/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:list")
    @Operation(summary = "获取登录日志列表", description = "权限接口。查询登录日志列表")
    public R<List<SysLoginLogPo>> listLoginLogs() {
        return logService.listLoginLogs();
    }

    @GetMapping("/login/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:query")
    @Operation(summary = "获取登录日志详情", description = "权限接口。按登录日志ID查询详情")
    public R<SysLoginLogPo> getLoginLog(
            @Parameter(description = "登录日志ID")
            @RequestParam Long id) {
        return logService.getLoginLog(id);
    }

    @DeleteMapping("/login/clean")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:delete")
    @Operation(summary = "清理登录日志", description = "权限接口。清空登录日志")
    public R<Boolean> cleanLoginLogs() {
        return logService.cleanLoginLogs();
    }

    @GetMapping("/login/statistics")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:query")
    @Operation(summary = "获取登录统计", description = "权限接口。查询登录日志统计数据")
    public R<Map<String, Object>> loginStatistics() {
        return logService.loginStatistics();
    }

    @GetMapping("/operation/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:operation:list")
    @Operation(summary = "获取操作日志列表", description = "权限接口。查询操作日志列表")
    public R<List<SysOperationLogPo>> listOperationLogs() {
        return logService.listOperationLogs();
    }

    @GetMapping("/operation/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:operation:query")
    @Operation(summary = "获取操作日志详情", description = "权限接口。按操作日志ID查询详情")
    public R<SysOperationLogPo> getOperationLog(
            @Parameter(description = "操作日志ID")
            @RequestParam Long id) {
        return logService.getOperationLog(id);
    }

    @DeleteMapping("/operation/clean")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:operation:delete")
    @Operation(summary = "清理操作日志", description = "权限接口。清空操作日志")
    public R<Boolean> cleanOperationLogs() {
        return logService.cleanOperationLogs();
    }
}
