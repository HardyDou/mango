package io.mango.job.core.service;

import io.mango.common.vo.PageResult;
import io.mango.job.api.command.SyncMangoJobInstanceCommand;
import io.mango.job.api.query.MangoJobInstancePageQuery;
import io.mango.job.api.query.MangoJobLogPageQuery;
import io.mango.job.api.query.MangoJobWorkerPageQuery;
import io.mango.job.api.vo.MangoJobEngineStatusVO;
import io.mango.job.api.vo.MangoJobHandlerVO;
import io.mango.job.api.vo.MangoJobInstanceVO;
import io.mango.job.api.vo.MangoJobLogDetailVO;
import io.mango.job.api.vo.MangoJobLogIndexVO;
import io.mango.job.api.vo.MangoJobWorkerSnapshotVO;

import java.util.List;

/**
 * Job 运行态查询服务。
 */
public interface IMangoJobQueryService {

    /**
     * 分页查询执行实例。
     *
     * @param query 查询条件
     * @return 执行实例分页结果
     */
    PageResult<MangoJobInstanceVO> pageInstances(MangoJobInstancePageQuery query);

    /**
     * 同步执行实例状态。
     *
     * @param command 同步命令
     * @return true 表示同步成功
     */
    Boolean syncInstances(SyncMangoJobInstanceCommand command);

    /**
     * 分页查询执行日志索引。
     *
     * @param query 查询条件
     * @return 日志索引分页结果
     */
    PageResult<MangoJobLogIndexVO> pageLogs(MangoJobLogPageQuery query);

    /**
     * 查询日志详情。
     *
     * @param id 日志索引 ID
     * @return 日志详情
     */
    MangoJobLogDetailVO detailLog(Long id);

    /**
     * 按执行实例查询日志详情。
     *
     * @param instanceId 执行实例 ID
     * @return 日志详情
     */
    MangoJobLogDetailVO detailInstanceLog(Long instanceId);

    /**
     * 分页查询 Worker 快照。
     *
     * @param query 查询条件
     * @return Worker 快照分页结果
     */
    PageResult<MangoJobWorkerSnapshotVO> pageWorkers(MangoJobWorkerPageQuery query);

    /**
     * 查询当前已注册处理器。
     *
     * @return 处理器清单
     */
    List<MangoJobHandlerVO> listHandlers();

    /**
     * 查询 Job 引擎运行状态。
     *
     * @return 引擎状态清单
     */
    List<MangoJobEngineStatusVO> listEngineStatus();
}
