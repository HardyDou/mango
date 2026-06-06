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

    PageResult<MangoJobInstanceVO> pageInstances(MangoJobInstancePageQuery query);

    Boolean syncInstances(SyncMangoJobInstanceCommand command);

    PageResult<MangoJobLogIndexVO> pageLogs(MangoJobLogPageQuery query);

    MangoJobLogDetailVO detailLog(Long id);

    PageResult<MangoJobWorkerSnapshotVO> pageWorkers(MangoJobWorkerPageQuery query);

    List<MangoJobHandlerVO> listHandlers();

    List<MangoJobEngineStatusVO> listEngineStatus();
}
