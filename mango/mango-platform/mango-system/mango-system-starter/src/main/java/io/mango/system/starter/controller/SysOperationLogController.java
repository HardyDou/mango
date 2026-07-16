package io.mango.system.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.system.api.SysOperationLogApi;
import io.mango.system.api.query.OperationLogPageQuery;
import io.mango.system.api.vo.SysOperationLogVO;
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
@Tag(name = "系统操作日志", description = "操作日志查询与清理接口")
public class SysOperationLogController implements SysOperationLogApi {

    private final ISysLogService logService;

    @Override
    @GetMapping("/operation/list")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:operation:list")
    @Operation(summary = "分页查询操作日志", description = "分页查询操作日志并返回处理结果")
    public R<PageResult<SysOperationLogVO>> page(@ParameterObject OperationLogPageQuery query) {
        return R.ok(logService.pageOperationLogs(query));
    }

    @Override
    @GetMapping("/operation/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:operation:query")
    @Operation(summary = "获取操作日志详情", description = "获取操作日志详情并返回处理结果")
    public R<SysOperationLogVO> get(@Parameter(description = "主键 ID", required = true) @RequestParam("id") Long id) {
        return R.ok(logService.getOperationLog(id));
    }

    @Override
    @DeleteMapping("/operation/clean")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:log:operation:delete")
    @Operation(summary = "清理操作日志", description = "清理操作日志并返回处理结果")
    public R<Boolean> clean(@Parameter(description = "日志保留天数", required = false) @RequestParam(value = "retentionDays", required = false) Integer retentionDays) {
        return R.ok(logService.cleanOperationLogs(retentionDays));
    }
}
