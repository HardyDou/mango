package io.mango.job.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.job.api.MangoJobApi;
import io.mango.job.api.command.SaveMangoJobDefinitionCommand;
import io.mango.job.api.command.TriggerMangoJobCommand;
import io.mango.job.api.command.UpdateMangoJobDefinitionStatusCommand;
import io.mango.job.api.query.MangoJobDefinitionPageQuery;
import io.mango.job.api.query.MangoJobInstancePageQuery;
import io.mango.job.api.query.MangoJobLogPageQuery;
import io.mango.job.api.query.MangoJobWorkerPageQuery;
import io.mango.job.api.vo.MangoJobDefinitionVO;
import io.mango.job.api.vo.MangoJobEngineStatusVO;
import io.mango.job.api.vo.MangoJobHandlerVO;
import io.mango.job.api.vo.MangoJobInstanceVO;
import io.mango.job.api.vo.MangoJobLogIndexVO;
import io.mango.job.api.vo.MangoJobWorkerSnapshotVO;
import io.mango.job.core.service.IMangoJobDefinitionService;
import io.mango.job.core.service.IMangoJobQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Mango Job 管理接口。
 */
@Validated
@RestController
@RequestMapping("/job")
@RequiredArgsConstructor
@Tag(name = "任务调度", description = "Mango 原生任务调度治理接口")
public class MangoJobController implements MangoJobApi {

    private final IMangoJobDefinitionService definitionService;

    private final IMangoJobQueryService queryService;

    @Override
    @GetMapping("/definitions/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "job:definition:list")
    @Operation(summary = "分页查询任务定义", description = "分页查询当前租户下的 Mango Job 任务定义")
    public R<PageResult<MangoJobDefinitionVO>> pageDefinitions(
            @Valid @ParameterObject MangoJobDefinitionPageQuery query) {
        return R.ok(definitionService.pageDefinitions(query));
    }

    @Override
    @GetMapping("/definitions/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "job:definition:query")
    @Operation(summary = "查询任务定义详情", description = "按任务定义 ID 查询详情")
    public R<MangoJobDefinitionVO> detailDefinition(
            @Parameter(description = "任务定义 ID", required = true)
            @NotNull(message = "任务 ID 不能为空")
            @RequestParam Long id) {
        return R.ok(definitionService.detailDefinition(id));
    }

    @Override
    @PostMapping("/definitions")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "job:definition:add")
    @Operation(summary = "新增任务定义", description = "创建 Mango Job 任务定义")
    public R<Long> createDefinition(@Valid @RequestBody SaveMangoJobDefinitionCommand command) {
        return R.ok(definitionService.createDefinition(command));
    }

    @Override
    @PutMapping("/definitions")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "job:definition:edit")
    @Operation(summary = "修改任务定义", description = "更新草稿状态的 Mango Job 任务定义")
    public R<Boolean> updateDefinition(@Valid @RequestBody SaveMangoJobDefinitionCommand command) {
        return R.ok(definitionService.updateDefinition(command));
    }

    @Override
    @PutMapping("/definitions/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "job:definition:status")
    @Operation(summary = "更新任务状态", description = "启用、暂停、禁用或退回草稿任务定义")
    public R<Boolean> updateDefinitionStatus(@Valid @RequestBody UpdateMangoJobDefinitionStatusCommand command) {
        return R.ok(definitionService.updateDefinitionStatus(command));
    }

    @Override
    @DeleteMapping("/definitions")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "job:definition:delete")
    @Operation(summary = "删除任务定义", description = "删除草稿状态的任务定义")
    public R<Boolean> deleteDefinition(
            @Parameter(description = "任务定义 ID", required = true)
            @NotNull(message = "任务 ID 不能为空")
            @RequestParam Long id) {
        return R.ok(definitionService.deleteDefinition(id));
    }

    @Override
    @PostMapping("/definitions/trigger")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "job:definition:trigger")
    @Operation(summary = "手动触发任务", description = "手动触发非草稿、非禁用状态的任务定义")
    public R<Long> triggerDefinition(@Valid @RequestBody TriggerMangoJobCommand command) {
        return R.ok(definitionService.triggerDefinition(command));
    }

    @Override
    @GetMapping("/instances/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "job:instance:list")
    @Operation(summary = "分页查询任务实例", description = "分页查询任务执行实例摘要")
    public R<PageResult<MangoJobInstanceVO>> pageInstances(
            @Valid @ParameterObject MangoJobInstancePageQuery query) {
        return R.ok(queryService.pageInstances(query));
    }

    @Override
    @GetMapping("/logs/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "job:log:list")
    @Operation(summary = "分页查询任务日志索引", description = "分页查询任务执行日志索引")
    public R<PageResult<MangoJobLogIndexVO>> pageLogs(@Valid @ParameterObject MangoJobLogPageQuery query) {
        return R.ok(queryService.pageLogs(query));
    }

    @Override
    @GetMapping("/workers/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "job:worker:list")
    @Operation(summary = "分页查询 Worker 快照", description = "分页查询任务执行 Worker 快照")
    public R<PageResult<MangoJobWorkerSnapshotVO>> pageWorkers(
            @Valid @ParameterObject MangoJobWorkerPageQuery query) {
        return R.ok(queryService.pageWorkers(query));
    }

    @Override
    @GetMapping("/handlers")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "job:handler:list")
    @Operation(summary = "查询处理器清单", description = "查询当前应用已注册的 Mango Job 处理器")
    public R<List<MangoJobHandlerVO>> listHandlers() {
        return R.ok(queryService.listHandlers());
    }

    @Override
    @GetMapping("/engines/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "job:engine:list")
    @Operation(summary = "查询引擎同步状态", description = "查询各调度引擎的任务同步状态汇总")
    public R<List<MangoJobEngineStatusVO>> listEngineStatus() {
        return R.ok(queryService.listEngineStatus());
    }
}
