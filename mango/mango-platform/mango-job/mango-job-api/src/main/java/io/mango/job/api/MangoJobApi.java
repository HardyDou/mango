package io.mango.job.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.job.api.command.CreateMangoJobWorkerCommand;
import io.mango.job.api.command.SaveMangoJobDefinitionCommand;
import io.mango.job.api.command.SaveMangoJobAlarmRuleCommand;
import io.mango.job.api.command.RegisterMangoJobWorkerCommand;
import io.mango.job.api.command.SyncMangoJobInstanceCommand;
import io.mango.job.api.command.TriggerMangoJobCommand;
import io.mango.job.api.command.UpdateMangoJobAlarmRuleStatusCommand;
import io.mango.job.api.command.UpdateMangoJobDefinitionStatusCommand;
import io.mango.job.api.command.UpdateMangoJobWorkerStatusCommand;
import io.mango.job.api.query.MangoJobAlarmRulePageQuery;
import io.mango.job.api.query.MangoJobDefinitionPageQuery;
import io.mango.job.api.query.MangoJobInstancePageQuery;
import io.mango.job.api.query.MangoJobLogPageQuery;
import io.mango.job.api.query.MangoJobWorkerPageQuery;
import io.mango.job.api.vo.MangoJobAlarmRuleVO;
import io.mango.job.api.vo.MangoJobDefinitionVO;
import io.mango.job.api.vo.MangoJobEngineStatusVO;
import io.mango.job.api.vo.MangoJobHandlerVO;
import io.mango.job.api.vo.MangoJobInstanceVO;
import io.mango.job.api.vo.MangoJobLogDetailVO;
import io.mango.job.api.vo.MangoJobLogIndexVO;
import io.mango.job.api.vo.MangoJobWorkerSnapshotVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Mango Job API 契约。
 */
@Validated
public interface MangoJobApi {

    /**
     * 分页查询任务定义。
     *
     * @param query 查询条件
     * @return 任务定义分页结果
     */
    R<PageResult<MangoJobDefinitionVO>> pageDefinitions(@Valid MangoJobDefinitionPageQuery query);

    /**
     * 查询任务定义详情。
     *
     * @param id 任务定义 ID
     * @return 任务定义详情
     */
    R<MangoJobDefinitionVO> detailDefinition(@NotNull(message = "任务 ID 不能为空") Long id);

    /**
     * 创建任务定义。
     *
     * @param command 保存命令
     * @return 新任务定义 ID
     */
    R<Long> createDefinition(@Valid SaveMangoJobDefinitionCommand command);

    /**
     * 更新任务定义。
     *
     * @param command 保存命令
     * @return true 表示更新成功
     */
    R<Boolean> updateDefinition(@Valid SaveMangoJobDefinitionCommand command);

    /**
     * 更新任务启停状态。
     *
     * @param command 状态更新命令
     * @return true 表示更新成功
     */
    R<Boolean> updateDefinitionStatus(@Valid UpdateMangoJobDefinitionStatusCommand command);

    /**
     * 删除任务定义。
     *
     * @param id 任务定义 ID
     * @return true 表示删除成功
     */
    R<Boolean> deleteDefinition(@NotNull(message = "任务 ID 不能为空") Long id);

    /**
     * 手动触发任务。
     *
     * @param command 触发命令
     * @return 执行实例 ID
     */
    R<Long> triggerDefinition(@Valid TriggerMangoJobCommand command);

    /**
     * 分页查询执行实例。
     *
     * @param query 查询条件
     * @return 执行实例分页结果
     */
    R<PageResult<MangoJobInstanceVO>> pageInstances(@Valid MangoJobInstancePageQuery query);

    /**
     * 同步执行实例状态。
     *
     * @param command 同步命令
     * @return true 表示同步成功
     */
    R<Boolean> syncInstances(@Valid SyncMangoJobInstanceCommand command);

    /**
     * 分页查询执行日志索引。
     *
     * @param query 查询条件
     * @return 日志索引分页结果
     */
    R<PageResult<MangoJobLogIndexVO>> pageLogs(@Valid MangoJobLogPageQuery query);

    /**
     * 查询日志详情。
     *
     * @param id 日志索引 ID
     * @return 日志详情
     */
    R<MangoJobLogDetailVO> detailLog(@NotNull(message = "日志 ID 不能为空") Long id);

    /**
     * 按执行实例查询日志详情。
     *
     * @param instanceId 执行实例 ID
     * @return 日志详情
     */
    R<MangoJobLogDetailVO> detailInstanceLog(@NotNull(message = "实例 ID 不能为空") Long instanceId);

    /**
     * 分页查询 Worker 快照。
     *
     * @param query 查询条件
     * @return Worker 快照分页结果
     */
    R<PageResult<MangoJobWorkerSnapshotVO>> pageWorkers(@Valid MangoJobWorkerPageQuery query);

    /**
     * 手动登记远程 Worker。
     *
     * @param command Worker 登记命令
     * @return Worker 快照 ID
     */
    R<Long> createWorker(@Valid CreateMangoJobWorkerCommand command);

    /**
     * 更新 Worker 治理状态。
     *
     * @param command Worker 状态命令
     * @return true 表示更新成功
     */
    R<Boolean> updateWorkerStatus(@Valid UpdateMangoJobWorkerStatusCommand command);

    /**
     * 注册 Worker 心跳和处理器能力。
     *
     * @param command Worker 注册命令
     * @return Worker 快照 ID
     */
    R<Long> registerWorker(@Valid RegisterMangoJobWorkerCommand command);

    /**
     * 查询当前已注册处理器。
     *
     * @return 处理器清单
     */
    R<List<MangoJobHandlerVO>> listHandlers();

    /**
     * 分页查询告警规则。
     *
     * @param query 查询条件
     * @return 告警规则分页结果
     */
    R<PageResult<MangoJobAlarmRuleVO>> pageAlarmRules(@Valid MangoJobAlarmRulePageQuery query);

    /**
     * 查询告警规则详情。
     *
     * @param id 告警规则 ID
     * @return 告警规则详情
     */
    R<MangoJobAlarmRuleVO> detailAlarmRule(@NotNull(message = "告警规则 ID 不能为空") Long id);

    /**
     * 创建告警规则。
     *
     * @param command 保存命令
     * @return 新告警规则 ID
     */
    R<Long> createAlarmRule(@Valid SaveMangoJobAlarmRuleCommand command);

    /**
     * 更新告警规则。
     *
     * @param command 保存命令
     * @return true 表示更新成功
     */
    R<Boolean> updateAlarmRule(@Valid SaveMangoJobAlarmRuleCommand command);

    /**
     * 更新告警规则启停状态。
     *
     * @param command 状态命令
     * @return true 表示更新成功
     */
    R<Boolean> updateAlarmRuleStatus(@Valid UpdateMangoJobAlarmRuleStatusCommand command);

    /**
     * 删除告警规则。
     *
     * @param id 告警规则 ID
     * @return true 表示删除成功
     */
    R<Boolean> deleteAlarmRule(@NotNull(message = "告警规则 ID 不能为空") Long id);

    /**
     * 查询 Job 引擎运行状态。
     *
     * @return 引擎状态清单
     */
    R<List<MangoJobEngineStatusVO>> listEngineStatus();
}
