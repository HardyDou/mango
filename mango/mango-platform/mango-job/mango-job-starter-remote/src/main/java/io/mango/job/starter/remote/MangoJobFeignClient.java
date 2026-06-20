package io.mango.job.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.job.api.MangoJobApi;
import io.mango.job.api.command.CreateMangoJobWorkerCommand;
import io.mango.job.api.command.RegisterMangoJobWorkerCommand;
import io.mango.job.api.command.SaveMangoJobAlarmRuleCommand;
import io.mango.job.api.command.SaveMangoJobDefinitionCommand;
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
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.List;

/**
 * Mango Job 远程调用客户端。
 */
@FeignClient(contextId = "mangoJobFeignClient", name = "mango-job", path = "/job",
        url = "${mango.job.native.job-center-feign-url:http://127.0.0.1}")
public interface MangoJobFeignClient extends MangoJobApi {

    @Override
    /**
     * 分页查询任务定义。
     *
     * @param query 查询条件
     * @return 任务定义分页结果
     */
    @GetMapping("/definitions/page")
    R<PageResult<MangoJobDefinitionVO>> pageDefinitions(@SpringQueryMap MangoJobDefinitionPageQuery query);

    @Override
    /**
     * 查询任务定义详情。
     *
     * @param id 任务定义 ID
     * @return 任务定义详情
     */
    @GetMapping("/definitions/detail")
    R<MangoJobDefinitionVO> detailDefinition(@RequestParam Long id);

    @Override
    /**
     * 创建任务定义。
     *
     * @param command 保存命令
     * @return 新任务定义 ID
     */
    @PostMapping("/definitions")
    R<Long> createDefinition(@RequestBody SaveMangoJobDefinitionCommand command);

    @Override
    /**
     * 更新任务定义。
     *
     * @param command 保存命令
     * @return true 表示更新成功
     */
    @PutMapping("/definitions")
    R<Boolean> updateDefinition(@RequestBody SaveMangoJobDefinitionCommand command);

    @Override
    /**
     * 更新任务启停状态。
     *
     * @param command 状态更新命令
     * @return true 表示更新成功
     */
    @PutMapping("/definitions/status")
    R<Boolean> updateDefinitionStatus(@RequestBody UpdateMangoJobDefinitionStatusCommand command);

    @Override
    /**
     * 删除任务定义。
     *
     * @param id 任务定义 ID
     * @return true 表示删除成功
     */
    @DeleteMapping("/definitions")
    R<Boolean> deleteDefinition(@RequestParam Long id);

    @Override
    /**
     * 手动触发任务。
     *
     * @param command 触发命令
     * @return 执行实例 ID
     */
    @PostMapping("/definitions/trigger")
    R<Long> triggerDefinition(@RequestBody TriggerMangoJobCommand command);

    @Override
    /**
     * 分页查询执行实例。
     *
     * @param query 查询条件
     * @return 执行实例分页结果
     */
    @GetMapping("/instances/page")
    R<PageResult<MangoJobInstanceVO>> pageInstances(@SpringQueryMap MangoJobInstancePageQuery query);

    @Override
    /**
     * 同步执行实例状态。
     *
     * @param command 同步命令
     * @return true 表示同步成功
     */
    @PostMapping("/instances/sync")
    R<Boolean> syncInstances(@RequestBody SyncMangoJobInstanceCommand command);

    @Override
    /**
     * 分页查询执行日志索引。
     *
     * @param query 查询条件
     * @return 日志索引分页结果
     */
    @GetMapping("/logs/page")
    R<PageResult<MangoJobLogIndexVO>> pageLogs(@SpringQueryMap MangoJobLogPageQuery query);

    @Override
    /**
     * 查询日志详情。
     *
     * @param id 日志索引 ID
     * @return 日志详情
     */
    @GetMapping("/logs/detail")
    R<MangoJobLogDetailVO> detailLog(@RequestParam Long id);

    @Override
    /**
     * 按执行实例查询日志详情。
     *
     * @param instanceId 执行实例 ID
     * @return 日志详情
     */
    @GetMapping("/instances/{instanceId}/logs")
    R<MangoJobLogDetailVO> detailInstanceLog(@PathVariable Long instanceId);

    @Override
    /**
     * 分页查询 Worker 快照。
     *
     * @param query 查询条件
     * @return Worker 快照分页结果
     */
    @GetMapping("/workers/page")
    R<PageResult<MangoJobWorkerSnapshotVO>> pageWorkers(@SpringQueryMap MangoJobWorkerPageQuery query);

    @Override
    /**
     * 手动登记远程 Worker。
     *
     * @param command Worker 登记命令
     * @return Worker 快照 ID
     */
    @PostMapping("/workers")
    R<Long> createWorker(@RequestBody CreateMangoJobWorkerCommand command);

    @Override
    /**
     * 更新 Worker 治理状态。
     *
     * @param command Worker 状态命令
     * @return true 表示更新成功
     */
    @PutMapping("/workers/status")
    R<Boolean> updateWorkerStatus(@RequestBody UpdateMangoJobWorkerStatusCommand command);

    @Override
    /**
     * 注册 Worker 心跳和处理器能力。
     *
     * @param command Worker 注册命令
     * @return Worker 快照 ID
     */
    @PostMapping("/internal/workers/register")
    R<Long> registerWorker(@RequestBody RegisterMangoJobWorkerCommand command);

    /**
     * 向指定 JobCenter 注册远程 Worker。
     *
     * @param jobCenterBaseUri JobCenter 基础地址
     * @param command Worker 注册命令
     * @return Worker 快照 ID
     */
    @PostMapping("/job/internal/workers/register")
    R<Long> registerWorker(URI jobCenterBaseUri, @RequestBody RegisterMangoJobWorkerCommand command);

    @Override
    /**
     * 查询当前已注册处理器。
     *
     * @return 处理器清单
     */
    @GetMapping("/handlers")
    R<List<MangoJobHandlerVO>> listHandlers();

    @Override
    /**
     * 分页查询告警规则。
     *
     * @param query 查询条件
     * @return 告警规则分页结果
     */
    @GetMapping("/alarm-rules/page")
    R<PageResult<MangoJobAlarmRuleVO>> pageAlarmRules(@SpringQueryMap MangoJobAlarmRulePageQuery query);

    @Override
    /**
     * 查询告警规则详情。
     *
     * @param id 告警规则 ID
     * @return 告警规则详情
     */
    @GetMapping("/alarm-rules/detail")
    R<MangoJobAlarmRuleVO> detailAlarmRule(@RequestParam Long id);

    @Override
    /**
     * 创建告警规则。
     *
     * @param command 保存命令
     * @return 新告警规则 ID
     */
    @PostMapping("/alarm-rules")
    R<Long> createAlarmRule(@RequestBody SaveMangoJobAlarmRuleCommand command);

    @Override
    /**
     * 更新告警规则。
     *
     * @param command 保存命令
     * @return true 表示更新成功
     */
    @PutMapping("/alarm-rules")
    R<Boolean> updateAlarmRule(@RequestBody SaveMangoJobAlarmRuleCommand command);

    @Override
    /**
     * 更新告警规则启停状态。
     *
     * @param command 状态命令
     * @return true 表示更新成功
     */
    @PutMapping("/alarm-rules/status")
    R<Boolean> updateAlarmRuleStatus(@RequestBody UpdateMangoJobAlarmRuleStatusCommand command);

    @Override
    /**
     * 删除告警规则。
     *
     * @param id 告警规则 ID
     * @return true 表示删除成功
     */
    @DeleteMapping("/alarm-rules")
    R<Boolean> deleteAlarmRule(@RequestParam Long id);

    @Override
    /**
     * 查询 Job 引擎运行状态。
     *
     * @return 引擎状态清单
     */
    @GetMapping("/engines/status")
    R<List<MangoJobEngineStatusVO>> listEngineStatus();
}
