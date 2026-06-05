package io.mango.job.starter.powerjob;

import tech.powerjob.client.PowerJobClient;
import tech.powerjob.client.ClientConfig;
import tech.powerjob.common.request.http.RunJobRequest;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.InstanceInfoDTO;
import tech.powerjob.common.response.PowerResultDTO;
import tech.powerjob.common.response.ResultDTO;

/**
 * PowerJob SDK 默认操作实现。
 */
public class PowerJobClientOperations implements IPowerJobClientOperations {

    private final PowerJobProperties properties;

    private volatile PowerJobClient client;

    public PowerJobClientOperations(PowerJobClient client) {
        this.client = client;
        this.properties = null;
    }

    public PowerJobClientOperations(PowerJobProperties properties) {
        this.properties = properties;
    }

    @Override
    public ResultDTO<Long> saveJob(SaveJobInfoRequest request) {
        return client().saveJob(request);
    }

    @Override
    public ResultDTO<Void> enableJob(Long jobId) {
        return client().enableJob(jobId);
    }

    @Override
    public ResultDTO<Void> disableJob(Long jobId) {
        return client().disableJob(jobId);
    }

    @Override
    public ResultDTO<Void> deleteJob(Long jobId) {
        return client().deleteJob(jobId);
    }

    @Override
    public PowerResultDTO<Long> runJob(RunJobRequest request) {
        return client().runJob(request);
    }

    @Override
    public ResultDTO<InstanceInfoDTO> fetchInstanceInfo(Long instanceId) {
        return client().fetchInstanceInfo(instanceId);
    }

    @Override
    public ResultDTO<?> fetchAllJob() {
        return client().fetchAllJob();
    }

    private PowerJobClient client() {
        PowerJobClient current = client;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (client == null) {
                ClientConfig config = new ClientConfig()
                        .setAddressList(properties.getServerAddresses())
                        .setAppName(properties.getAppName())
                        .setPassword(properties.getPassword())
                        .setConnectionTimeout(properties.getConnectionTimeoutMillis())
                        .setReadTimeout(properties.getReadTimeoutMillis())
                        .setWriteTimeout(properties.getWriteTimeoutMillis());
                client = new PowerJobClient(config);
            }
            return client;
        }
    }
}
