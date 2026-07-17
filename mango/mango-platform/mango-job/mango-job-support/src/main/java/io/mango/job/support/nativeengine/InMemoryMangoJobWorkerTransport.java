package io.mango.job.support.nativeengine;

import io.mango.job.api.enums.JobTransportType;
import io.mango.job.api.vo.MangoJobWorkerExecuteResultVO;

/**
 * Dispatches a job handler inside the current JVM.
 */
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
    public MangoJobWorkerExecuteResultVO execute(MangoJobWorkerDispatchContext context) {
        return workerExecutor.execute(context.getCommand(), context.getWorkerAddress());
    }
}
