package io.mango.job.support.nativeengine;

import io.mango.job.api.command.MangoJobWorkerExecuteCommand;
import io.mango.job.api.vo.MangoJobWorkerExecuteResultVO;

/**
 * Worker 内部执行服务。
 */
public interface IMangoJobWorkerExecutor {

    /**
     * 在当前 Worker 进程执行任务。
     *
     * @param command Worker 执行命令
     * @return Worker 执行结果和捕获日志
     */
    MangoJobWorkerExecuteResultVO execute(MangoJobWorkerExecuteCommand command);
}
