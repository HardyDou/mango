package io.mango.job.starter.remote;

import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.job.api.enums.JobCode;
import io.mango.job.api.enums.JobTransportType;
import io.mango.job.api.vo.MangoJobWorkerExecuteResultVO;
import io.mango.job.support.nativeengine.IMangoJobWorkerTransport;
import io.mango.job.support.nativeengine.MangoJobWorkerDispatchContext;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * Mango 内部 HTTP Worker 分发通道。
 */
public class HttpInternalMangoJobWorkerTransport implements IMangoJobWorkerTransport {

    private final MangoJobDynamicHttpClient dynamicHttpClient;

    public HttpInternalMangoJobWorkerTransport(MangoJobDynamicHttpClient dynamicHttpClient) {
        this.dynamicHttpClient = dynamicHttpClient;
    }

    @Override
    public JobTransportType transportType() {
        return JobTransportType.HTTP_INTERNAL;
    }

    @Override
    public MangoJobWorkerExecuteResultVO execute(MangoJobWorkerDispatchContext request) {
        URI workerBaseUri = URI.create(request.getWorkerAddress());
        R<MangoJobWorkerExecuteResultVO> response = dynamicHttpClient.executeWorker(workerBaseUri, request.getCommand());
        Require.notNull(response, JobCode.JOB_INVALID, "Worker HTTP_INTERNAL 调用无响应");
        Require.isTrue(response.isSuccess(), JobCode.JOB_INVALID, response.getMsg());
        MangoJobWorkerExecuteResultVO data = response.getData();
        Require.notNull(data, JobCode.JOB_INVALID, "Worker HTTP_INTERNAL 执行结果为空");
        if (!StringUtils.hasText(data.getWorkerAddress())) {
            data.setWorkerAddress(request.getWorkerAddress());
        }
        return data;
    }
}
