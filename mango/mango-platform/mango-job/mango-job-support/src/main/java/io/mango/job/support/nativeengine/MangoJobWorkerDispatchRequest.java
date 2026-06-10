package io.mango.job.support.nativeengine;

import io.mango.job.api.command.MangoJobWorkerExecuteCommand;
import lombok.Getter;

/**
 * Mango Job Worker 分发请求。
 */
@Getter
public class MangoJobWorkerDispatchRequest {

    private final String workerAddress;

    private final MangoJobWorkerExecuteCommand command;

    public MangoJobWorkerDispatchRequest(String workerAddress,
                                         MangoJobWorkerExecuteCommand command) {
        this.workerAddress = workerAddress;
        this.command = command;
    }
}
