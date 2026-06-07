package io.mango.job.starter.remote;

import io.mango.common.result.R;
import io.mango.job.api.MangoJobWorkerApi;
import io.mango.job.api.command.MangoJobWorkerExecuteCommand;
import io.mango.job.api.vo.MangoJobWorkerExecuteResultVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;

/**
 * Mango Job Worker 远程执行客户端。
 */
@FeignClient(contextId = "mangoJobWorkerFeignClient", name = "mango-job", path = "/job",
        url = "${mango.job.native.worker-feign-url:http://127.0.0.1}")
public interface MangoJobWorkerFeignClient extends MangoJobWorkerApi {

    /**
     * 向指定 Worker 派发执行命令。
     *
     * @param workerBaseUri Worker 基础地址
     * @param command Worker 执行命令
     * @return Worker 执行结果
     */
    @PostMapping("/job/internal/workers/execute")
    R<MangoJobWorkerExecuteResultVO> execute(URI workerBaseUri, @RequestBody MangoJobWorkerExecuteCommand command);
}
