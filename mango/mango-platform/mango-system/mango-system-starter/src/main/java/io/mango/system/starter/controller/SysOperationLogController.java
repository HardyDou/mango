package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.system.api.SysOperationLogApi;
import io.mango.system.api.po.SysOperationLogPo;
import io.mango.system.api.query.OperationLogPageQuery;
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

@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
@Tag(name = "系统操作日志", description = "操作日志查询与清理接口")
public class SysOperationLogController implements SysOperationLogApi {

    private final ISysLogService logService;

    @Override
    public R<List<SysOperationLogPo>> list() {
        return logService.listOperationLogs();
    }

    @Override
    @GetMapping("/operation/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:operation:list")
    @Operation(summary = "分页查询操作日志", description = "权限接口。按关键字、用户名、状态和时间范围分页查询操作日志")
    public R<PageResult<SysOperationLogPo>> page(@ParameterObject OperationLogPageQuery query) {
        return logService.pageOperationLogs(query);
    }

    @Override
    @GetMapping("/operation/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:operation:query")
    @Operation(summary = "获取操作日志详情", description = "权限接口。按操作日志ID查询详情")
    public R<SysOperationLogPo> get(
            @Parameter(description = "操作日志ID")
            @RequestParam Long id) {
        return logService.getOperationLog(id);
    }

    @Override
    public R<Boolean> record(SysOperationLogPo log) {
        return logService.recordOperationLog(log);
    }

    @Override
    @DeleteMapping("/operation/clean")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:operation:delete")
    @Operation(summary = "清理操作日志", description = "权限接口。按保留天数清理操作日志；不传保留天数时清理当前可见范围内全部操作日志")
    public R<Boolean> clean(
            @Parameter(description = "保留天数。只删除早于该天数的日志；不传或小于等于0表示清理全部")
            @RequestParam(required = false) Integer retentionDays) {
        return logService.cleanOperationLogs(retentionDays);
    }
}
