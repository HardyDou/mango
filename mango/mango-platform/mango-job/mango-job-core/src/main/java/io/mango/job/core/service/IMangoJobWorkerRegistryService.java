package io.mango.job.core.service;

import io.mango.job.api.command.CreateMangoJobWorkerCommand;
import io.mango.job.api.command.RegisterMangoJobWorkerCommand;
import io.mango.job.api.command.UpdateMangoJobWorkerStatusCommand;

/**
 * Mango Job Worker 注册服务。
 */
public interface IMangoJobWorkerRegistryService {

    /**
     * 注册 Worker 心跳和处理器能力。
     *
     * @param command Worker 注册命令
     * @return Worker 快照 ID
     */
    Long registerWorker(RegisterMangoJobWorkerCommand command);

    /**
     * 手动登记远程 Worker。
     *
     * @param command Worker 登记命令
     * @return Worker 快照 ID
     */
    Long createWorker(CreateMangoJobWorkerCommand command);

    /**
     * 更新 Worker 治理状态。
     *
     * @param command 状态更新命令
     * @return true 表示更新成功
     */
    Boolean updateWorkerStatus(UpdateMangoJobWorkerStatusCommand command);
}
