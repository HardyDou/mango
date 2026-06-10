package io.mango.job.starter.remote;

import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.job.api.enums.JobTransportType;
import io.mango.job.api.vo.MangoJobWorkerExecuteResultVO;
import io.mango.job.support.nativeengine.IMangoJobWorkerTransport;
import io.mango.job.support.nativeengine.MangoJobWorkerDispatchRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * Mango 内部 HTTP Worker 分发通道。
 */
@Service
public class HttpInternalMangoJobWorkerTransport implements IMangoJobWorkerTransport {

    private final MangoJobWorkerFeignClient workerFeignClient;

    public HttpInternalMangoJobWorkerTransport(MangoJobWorkerFeignClient workerFeignClient) {
        this.workerFeignClient = workerFeignClient;
    }

    @Override
    public JobTransportType transportType() {
        return JobTransportType.HTTP_INTERNAL;
    }

    @Override
    public MangoJobWorkerExecuteResultVO execute(MangoJobWorkerDispatchRequest request) {
        URI workerBaseUri = URI.create(request.getWorkerAddress());
        R<MangoJobWorkerExecuteResultVO> response = workerFeignClient.execute(workerBaseUri, request.getCommand());
        Require.notNull(response, "Worker HTTP_INTERNAL 调用无响应");
        Require.isTrue(response.isSuccess(), response.getMsg());
        MangoJobWorkerExecuteResultVO data = response.getData();
        Require.notNull(data, "Worker HTTP_INTERNAL 执行结果为空");
        if (!StringUtils.hasText(data.getWorkerAddress())) {
            data.setWorkerAddress(request.getWorkerAddress());
        }
        return data;
    }
}
