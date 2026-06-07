package io.mango.job.api;

import io.mango.common.result.R;
import io.mango.job.api.command.MangoJobWorkerExecuteCommand;
import io.mango.job.api.vo.MangoJobWorkerExecuteResultVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Mango Job Worker internal API contract.
 */
@Validated
public interface MangoJobWorkerApi {

    /**
     * 执行 JobCenter 派发到当前 Worker 的任务。
     *
     * @param command Worker 执行命令
     * @return Worker 执行结果和捕获日志
     */
    @PostMapping("/internal/workers/execute")
    R<MangoJobWorkerExecuteResultVO> execute(@Valid @RequestBody MangoJobWorkerExecuteCommand command);
}
