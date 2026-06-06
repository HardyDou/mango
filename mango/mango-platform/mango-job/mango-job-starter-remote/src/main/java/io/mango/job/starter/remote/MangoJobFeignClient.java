package io.mango.job.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.job.api.MangoJobApi;
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
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Mango Job 远程调用客户端。
 */
@FeignClient(name = "mango-job", path = "/job")
public interface MangoJobFeignClient extends MangoJobApi {

    @Override
    @GetMapping("/definitions/page")
    R<PageResult<MangoJobDefinitionVO>> pageDefinitions(@SpringQueryMap MangoJobDefinitionPageQuery query);

    @Override
    @GetMapping("/definitions/detail")
    R<MangoJobDefinitionVO> detailDefinition(@RequestParam Long id);

    @Override
    @PostMapping("/definitions")
    R<Long> createDefinition(@RequestBody SaveMangoJobDefinitionCommand command);

    @Override
    @PutMapping("/definitions")
    R<Boolean> updateDefinition(@RequestBody SaveMangoJobDefinitionCommand command);

    @Override
    @PutMapping("/definitions/status")
    R<Boolean> updateDefinitionStatus(@RequestBody UpdateMangoJobDefinitionStatusCommand command);

    @Override
    @DeleteMapping("/definitions")
    R<Boolean> deleteDefinition(@RequestParam Long id);

    @Override
    @PostMapping("/definitions/trigger")
    R<Long> triggerDefinition(@RequestBody TriggerMangoJobCommand command);

    @Override
    @GetMapping("/instances/page")
    R<PageResult<MangoJobInstanceVO>> pageInstances(@SpringQueryMap MangoJobInstancePageQuery query);

    @Override
    @PostMapping("/instances/sync")
    R<Boolean> syncInstances(@RequestBody SyncMangoJobInstanceCommand command);

    @Override
    @GetMapping("/logs/page")
    R<PageResult<MangoJobLogIndexVO>> pageLogs(@SpringQueryMap MangoJobLogPageQuery query);

    @Override
    @GetMapping("/logs/detail")
    R<MangoJobLogDetailVO> detailLog(@RequestParam Long id);

    @Override
    @GetMapping("/workers/page")
    R<PageResult<MangoJobWorkerSnapshotVO>> pageWorkers(@SpringQueryMap MangoJobWorkerPageQuery query);

    @Override
    @GetMapping("/handlers")
    R<List<MangoJobHandlerVO>> listHandlers();

    @Override
    @GetMapping("/engines/status")
    R<List<MangoJobEngineStatusVO>> listEngineStatus();
}
