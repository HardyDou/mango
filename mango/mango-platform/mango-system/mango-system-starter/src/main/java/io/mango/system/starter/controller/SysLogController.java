package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.system.api.po.SysLoginLogPo;
import io.mango.system.api.po.SysOperationLogPo;
import io.mango.system.core.service.ISysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
public class SysLogController {

    private final ISysLogService logService;

    @GetMapping("/login/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:list")
    public R<List<SysLoginLogPo>> listLoginLogs() {
        return logService.listLoginLogs();
    }

    @GetMapping("/login/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:query")
    public R<SysLoginLogPo> getLoginLog(@RequestParam Long id) {
        return logService.getLoginLog(id);
    }

    @DeleteMapping("/login/clean")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:delete")
    public R<Boolean> cleanLoginLogs() {
        return logService.cleanLoginLogs();
    }

    @GetMapping("/login/statistics")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:login:query")
    public R<Map<String, Object>> loginStatistics() {
        return logService.loginStatistics();
    }

    @GetMapping("/operation/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:operation:list")
    public R<List<SysOperationLogPo>> listOperationLogs() {
        return logService.listOperationLogs();
    }

    @GetMapping("/operation/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:operation:query")
    public R<SysOperationLogPo> getOperationLog(@RequestParam Long id) {
        return logService.getOperationLog(id);
    }

    @DeleteMapping("/operation/clean")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:operation:delete")
    public R<Boolean> cleanOperationLogs() {
        return logService.cleanOperationLogs();
    }
}
