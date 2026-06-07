package io.mango.job.support.nativeengine;

import io.mango.job.api.enums.JobTransportType;
import io.mango.job.api.vo.MangoJobWorkerExecuteResultVO;
import org.springframework.stereotype.Service;

/**
 * 同 JVM Mango Job Worker 分发通道。
 */
@Service
public class InMemoryMangoJobWorkerTransport implements IMangoJobWorkerTransport {

    private final MangoJobWorkerExecutor workerExecutor;

    public InMemoryMangoJobWorkerTransport(MangoJobWorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor;
    }

    @Override
    public JobTransportType transportType() {
        return JobTransportType.IN_MEMORY;
    }

    @Override
    public MangoJobWorkerExecuteResultVO execute(MangoJobWorkerDispatchRequest request) {
        return workerExecutor.execute(request.getCommand(), request.getWorkerAddress());
    }
}
