package io.mango.job.core.service.nativeengine;

import io.mango.common.result.Require;
import io.mango.job.api.enums.JobCode;
import io.mango.job.api.enums.JobTransportType;
import io.mango.job.support.nativeengine.IMangoJobWorkerTransport;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Mango Job Worker transport 注册表。
 */
public class MangoJobWorkerTransportRegistry {

    private final Map<JobTransportType, IMangoJobWorkerTransport> transports = new EnumMap<>(JobTransportType.class);

    public MangoJobWorkerTransportRegistry(List<IMangoJobWorkerTransport> transportList) {
        for (IMangoJobWorkerTransport transport : transportList) {
            transports.put(transport.transportType(), transport);
        }
    }

    public IMangoJobWorkerTransport requireTransport(JobTransportType type) {
        IMangoJobWorkerTransport transport = transports.get(type);
        Require.notNull(transport, JobCode.JOB_INVALID, "Job Worker 通信方式未注册：" + type);
        return transport;
    }
}
