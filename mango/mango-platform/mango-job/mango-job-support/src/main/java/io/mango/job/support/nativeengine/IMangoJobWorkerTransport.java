package io.mango.job.support.nativeengine;

import io.mango.job.api.enums.JobTransportType;
import io.mango.job.api.vo.MangoJobWorkerExecuteResultVO;

/**
 * Mango JobCenter 到 Worker 的分发通道。
 */
public interface IMangoJobWorkerTransport {

    /**
     * 支持的通信方式。
     *
     * @return 通信方式
     */
    JobTransportType transportType();

    /**
     * 执行 Worker 任务。
     *
     * @param request Worker 分发请求
     * @return Worker 执行结果
     */
    MangoJobWorkerExecuteResultVO execute(MangoJobWorkerDispatchRequest request);
}
