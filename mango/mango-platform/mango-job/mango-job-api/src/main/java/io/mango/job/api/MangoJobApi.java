package io.mango.job.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.job.api.command.SaveMangoJobDefinitionCommand;
import io.mango.job.api.command.SyncMangoJobInstanceCommand;
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

    R<PageResult<MangoJobDefinitionVO>> pageDefinitions(@Valid MangoJobDefinitionPageQuery query);

    R<MangoJobDefinitionVO> detailDefinition(@NotNull(message = "任务 ID 不能为空") Long id);

    R<Long> createDefinition(@Valid SaveMangoJobDefinitionCommand command);

    R<Boolean> updateDefinition(@Valid SaveMangoJobDefinitionCommand command);

    R<Boolean> updateDefinitionStatus(@Valid UpdateMangoJobDefinitionStatusCommand command);

    R<Boolean> deleteDefinition(@NotNull(message = "任务 ID 不能为空") Long id);

    R<Long> triggerDefinition(@Valid TriggerMangoJobCommand command);

    R<PageResult<MangoJobInstanceVO>> pageInstances(@Valid MangoJobInstancePageQuery query);

    R<Boolean> syncInstances(@Valid SyncMangoJobInstanceCommand command);

    R<PageResult<MangoJobLogIndexVO>> pageLogs(@Valid MangoJobLogPageQuery query);

    R<MangoJobLogDetailVO> detailLog(@NotNull(message = "日志 ID 不能为空") Long id);

    R<PageResult<MangoJobWorkerSnapshotVO>> pageWorkers(@Valid MangoJobWorkerPageQuery query);

    R<List<MangoJobHandlerVO>> listHandlers();

    R<List<MangoJobEngineStatusVO>> listEngineStatus();
}
