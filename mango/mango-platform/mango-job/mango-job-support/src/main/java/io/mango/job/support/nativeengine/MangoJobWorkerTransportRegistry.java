package io.mango.job.support.nativeengine;

import io.mango.common.result.Require;
import io.mango.job.api.enums.JobTransportType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Mango Job Worker transport 注册表。
 */
@Service
public class MangoJobWorkerTransportRegistry {

    private final Map<JobTransportType, IMangoJobWorkerTransport> transports = new EnumMap<>(JobTransportType.class);

    public MangoJobWorkerTransportRegistry(List<IMangoJobWorkerTransport> transportList) {
        for (IMangoJobWorkerTransport transport : transportList) {
            transports.put(transport.transportType(), transport);
        }
    }

    public IMangoJobWorkerTransport requireTransport(JobTransportType type) {
        IMangoJobWorkerTransport transport = transports.get(type);
        Require.notNull(transport, "Job Worker 通信方式未注册：" + type);
        return transport;
    }
}
