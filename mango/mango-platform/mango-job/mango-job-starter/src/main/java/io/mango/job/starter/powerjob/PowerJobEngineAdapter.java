package io.mango.job.starter.powerjob;

import io.mango.job.api.enums.JobDefinitionStatus;
import io.mango.job.api.enums.JobEngineType;
import io.mango.job.api.enums.JobInstanceStatus;
import io.mango.job.api.enums.JobScheduleType;
import io.mango.job.api.enums.JobType;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.service.engine.IMangoJobEngine;
import io.mango.job.core.service.engine.MangoJobEngineInstanceSnapshot;
import io.mango.job.core.service.engine.MangoJobEngineRequest;
import io.mango.job.core.service.engine.MangoJobEngineResult;
import io.mango.job.core.service.engine.MangoJobInstanceImportRequest;
import io.mango.job.core.service.engine.MangoJobLogRequest;
import io.mango.job.core.service.engine.MangoJobLogResult;
import io.mango.job.core.service.engine.MangoJobTriggerRequest;
import org.springframework.util.StringUtils;
import tech.powerjob.common.enums.DispatchStrategy;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.request.http.RunJobRequest;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.InstanceInfoDTO;
import tech.powerjob.common.response.ResultDTO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * PowerJob 引擎适配器。
 */
public class PowerJobEngineAdapter implements IMangoJobEngine {

    private final IPowerJobClientOperations client;

    private final PowerJobProperties properties;

    private final IPowerJobNativeLogReader nativeLogReader;

    private final IPowerJobInstanceReader instanceReader;

    public PowerJobEngineAdapter(IPowerJobClientOperations client, PowerJobProperties properties) {
        this(client, properties, null, null);
    }

    public PowerJobEngineAdapter(IPowerJobClientOperations client,
                                 PowerJobProperties properties,
                                 IPowerJobNativeLogReader nativeLogReader) {
        this(client, properties, nativeLogReader, null);
    }

    public PowerJobEngineAdapter(IPowerJobClientOperations client,
                                 PowerJobProperties properties,
                                 IPowerJobNativeLogReader nativeLogReader,
                                 IPowerJobInstanceReader instanceReader) {
        this.client = client;
        this.properties = properties;
        this.nativeLogReader = nativeLogReader;
        this.instanceReader = instanceReader;
    }

    @Override
    public String engineType() {
        return JobEngineType.POWERJOB.name();
    }

    @Override
    public MangoJobEngineResult syncDefinition(MangoJobEngineRequest request) {
        try {
            MangoJobDefinitionEntity definition = request.getDefinition();
            SaveJobInfoRequest powerRequest = toSaveRequest(definition);
            ResultDTO<Long> result = client.saveJob(powerRequest);
            if (!result.isSuccess()) {
                return MangoJobEngineResult.failed(result.getMessage());
            }
            Long engineJobId = result.getData();
            if (targetEnabled(definition)) {
                ResultDTO<Void> enableResult = client.enableJob(engineJobId);
                if (!enableResult.isSuccess()) {
                    return MangoJobEngineResult.failed(enableResult.getMessage());
                }
            } else if (definition.getEngineJobId() != null) {
                ResultDTO<Void> disableResult = client.disableJob(engineJobId);
                if (!disableResult.isSuccess()) {
                    return MangoJobEngineResult.failed(disableResult.getMessage());
                }
            }
            return MangoJobEngineResult.success(String.valueOf(properties.getAppId()), String.valueOf(engineJobId));
        } catch (RuntimeException ex) {
            return MangoJobEngineResult.failed(ex.getMessage());
        }
    }

    @Override
    public MangoJobEngineResult deleteDefinition(MangoJobEngineRequest request) {
        try {
            String engineJobId = request.getDefinition().getEngineJobId();
            if (!StringUtils.hasText(engineJobId)) {
                return MangoJobEngineResult.success();
            }
            ResultDTO<Void> result = client.deleteJob(Long.valueOf(engineJobId));
            return result.isSuccess() ? MangoJobEngineResult.success() : MangoJobEngineResult.failed(result.getMessage());
        } catch (RuntimeException ex) {
            return MangoJobEngineResult.failed(ex.getMessage());
        }
    }

    @Override
    public MangoJobEngineResult trigger(MangoJobTriggerRequest request) {
        try {
            MangoJobDefinitionEntity definition = request.getDefinition();
            if (!StringUtils.hasText(definition.getEngineJobId())) {
                return MangoJobEngineResult.failed("任务尚未同步到 PowerJob");
            }
            RunJobRequest runRequest = new RunJobRequest()
                    .setAppId(properties.getAppId())
                    .setJobId(Long.valueOf(definition.getEngineJobId()))
                    .setInstanceParams(PowerJobMangoPayload.instanceParams(
                            definition, request.getInstance(), request.getBatchNo(), request.getParamValue()))
                    .setOuterKey(request.getBatchNo());
            ResultDTO<Long> result = client.runJob(runRequest);
            if (!result.isSuccess()) {
                return MangoJobEngineResult.failed(result.getMessage());
            }
            return MangoJobEngineResult.triggerSuccess(String.valueOf(result.getData()));
        } catch (RuntimeException ex) {
            return MangoJobEngineResult.failed(ex.getMessage());
        }
    }

    @Override
    public MangoJobEngineResult refreshInstance(MangoJobTriggerRequest request) {
        try {
            String engineInstanceId = request.getInstance().getEngineInstanceId();
            if (!StringUtils.hasText(engineInstanceId)) {
                return MangoJobEngineResult.success();
            }
            ResultDTO<InstanceInfoDTO> result = client.fetchInstanceInfo(Long.valueOf(engineInstanceId));
            if (!result.isSuccess()) {
                return MangoJobEngineResult.failed(result.getMessage());
            }
            InstanceInfoDTO info = result.getData();
            if (info == null) {
                return MangoJobEngineResult.failed("PowerJob 实例不存在：" + engineInstanceId);
            }
            return MangoJobEngineResult.instanceSuccess(
                    toMangoInstanceStatus(info.getStatus()),
                    toLocalDateTime(info.getActualTriggerTime()),
                    toLocalDateTime(info.getFinishedTime()),
                    duration(info),
                    errorSummary(info),
                    info.getTaskTrackerAddress());
        } catch (RuntimeException ex) {
            return MangoJobEngineResult.failed(ex.getMessage());
        }
    }

    @Override
    public List<MangoJobEngineInstanceSnapshot> importInstances(MangoJobInstanceImportRequest request) {
        if (instanceReader == null || request == null || request.getDefinition() == null
                || !StringUtils.hasText(request.getDefinition().getEngineJobId())) {
            return List.of();
        }
        Long jobId = Long.valueOf(request.getDefinition().getEngineJobId());
        return instanceReader.readRecentInstances(jobId, request.getTriggerTimeStart(), request.getTriggerTimeEnd(),
                        request.getLimit())
                .stream()
                .map(this::toSnapshot)
                .toList();
    }

    @Override
    public MangoJobLogResult fetchLog(MangoJobLogRequest request) {
        try {
            String engineInstanceId = request.getLogIndex().getEngineInstanceId();
            if (!StringUtils.hasText(engineInstanceId)) {
                return MangoJobLogResult.failed("POWERJOB_NATIVE_LOG", "日志索引缺少 PowerJob 实例 ID");
            }
            ResultDTO<InstanceInfoDTO> result = client.fetchInstanceInfo(Long.valueOf(engineInstanceId));
            if (!result.isSuccess()) {
                return MangoJobLogResult.failed("POWERJOB_NATIVE_LOG", result.getMessage());
            }
            InstanceInfoDTO info = result.getData();
            if (info == null) {
                return MangoJobLogResult.failed("POWERJOB_NATIVE_LOG", "PowerJob 实例不存在：" + engineInstanceId);
            }
            String engineResult = info.getResult();
            if (nativeLogReader == null) {
                return MangoJobLogResult.unavailable(
                        "POWERJOB_NATIVE_LOG",
                        "执行日志读取器未启用，当前执行结果仅作为兜底信息返回",
                        engineResult);
            }
            PowerJobNativeLog nativeLog = nativeLogReader.readInstanceLog(Long.valueOf(engineInstanceId));
            if (!nativeLog.isAvailable()) {
                return MangoJobLogResult.unavailable("POWERJOB_NATIVE_LOG", nativeLog.getErrorSummary(), engineResult);
            }
            return MangoJobLogResult.success("POWERJOB_NATIVE_LOG", nativeLog.getContent(), engineResult);
        } catch (RuntimeException ex) {
            return MangoJobLogResult.failed("POWERJOB_NATIVE_LOG", ex.getMessage());
        }
    }

    @Override
    public MangoJobEngineResult health() {
        try {
            ResultDTO<?> result = client.fetchAllJob();
            return result.isSuccess() ? MangoJobEngineResult.success() : MangoJobEngineResult.failed(result.getMessage());
        } catch (RuntimeException ex) {
            return MangoJobEngineResult.failed(ex.getMessage());
        }
    }

    private SaveJobInfoRequest toSaveRequest(MangoJobDefinitionEntity definition) {
        SaveJobInfoRequest request = new SaveJobInfoRequest();
        if (StringUtils.hasText(definition.getEngineJobId())) {
            request.setId(Long.valueOf(definition.getEngineJobId()));
        }
        request.setAppId(properties.getAppId());
        request.setJobName(definition.getJobName());
        request.setJobDescription(definition.getJobCode());
        request.setJobParams(PowerJobMangoPayload.jobParams(definition));
        request.setTimeExpressionType(timeExpressionType(definition.getScheduleType()));
        request.setTimeExpression(timeExpression(definition));
        request.setExecuteType(ExecuteType.STANDALONE);
        request.setProcessorType(processorType(definition.getJobType()));
        request.setProcessorInfo(MangoPowerJobProcessor.PROCESSOR_NAME);
        request.setMaxInstanceNum(properties.getMaxInstanceNum());
        request.setConcurrency(properties.getConcurrency());
        request.setInstanceTimeLimit(timeoutMillis(definition));
        request.setInstanceRetryNum(0);
        request.setTaskRetryNum(0);
        request.setEnable(targetEnabled(definition));
        request.setDispatchStrategy(DispatchStrategy.HEALTH_FIRST);
        return request;
    }

    private TimeExpressionType timeExpressionType(String scheduleType) {
        JobScheduleType type = JobScheduleType.valueOf(scheduleType);
        return switch (type) {
            case CRON -> TimeExpressionType.CRON;
            case FIXED_RATE -> TimeExpressionType.FIXED_RATE;
            case MANUAL -> TimeExpressionType.API;
            case ONE_TIME -> TimeExpressionType.API;
        };
    }

    private String timeExpression(MangoJobDefinitionEntity definition) {
        if (JobScheduleType.MANUAL.name().equals(definition.getScheduleType())) {
            return null;
        }
        return definition.getScheduleExpression();
    }

    private ProcessorType processorType(String jobType) {
        JobType.valueOf(jobType);
        return ProcessorType.BUILT_IN;
    }

    private Long timeoutMillis(MangoJobDefinitionEntity definition) {
        if (definition.getTimeoutSeconds() == null) {
            return 0L;
        }
        return definition.getTimeoutSeconds() * 1000L;
    }

    private boolean targetEnabled(MangoJobDefinitionEntity definition) {
        return JobDefinitionStatus.ENABLED.name().equals(definition.getStatus());
    }

    private String toMangoInstanceStatus(int status) {
        InstanceStatus instanceStatus = InstanceStatus.of(status);
        if (instanceStatus == InstanceStatus.WAITING_DISPATCH
                || instanceStatus == InstanceStatus.WAITING_WORKER_RECEIVE) {
            return JobInstanceStatus.WAITING.name();
        }
        if (instanceStatus == InstanceStatus.RUNNING) {
            return JobInstanceStatus.RUNNING.name();
        }
        if (instanceStatus == InstanceStatus.SUCCEED) {
            return JobInstanceStatus.SUCCESS.name();
        }
        if (instanceStatus == InstanceStatus.CANCELED || instanceStatus == InstanceStatus.STOPPED) {
            return JobInstanceStatus.CANCELED.name();
        }
        return JobInstanceStatus.FAILED.name();
    }

    private LocalDateTime toLocalDateTime(Long millis) {
        if (millis == null || millis <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

    private MangoJobEngineInstanceSnapshot toSnapshot(PowerJobInstanceInfoEntity entity) {
        MangoJobEngineInstanceSnapshot snapshot = new MangoJobEngineInstanceSnapshot();
        Long engineInstanceId = entity.getInstanceId() == null ? entity.getId() : entity.getInstanceId();
        snapshot.setEngineInstanceId(engineInstanceId == null ? null : String.valueOf(engineInstanceId));
        snapshot.setTriggerTime(toLocalDateTime(resolveTriggerTime(entity)));
        snapshot.setStartTime(toLocalDateTime(entity.getActualTriggerTime()));
        snapshot.setEndTime(toLocalDateTime(entity.getFinishedTime()));
        snapshot.setStatus(toMangoInstanceStatus(entity.getStatus()));
        snapshot.setDurationMillis(duration(entity));
        snapshot.setErrorSummary(errorSummary(entity));
        snapshot.setWorkerAddress(entity.getTaskTrackerAddress());
        snapshot.setTriggerBatchNo(entity.getOuterKey());
        return snapshot;
    }

    private Long resolveTriggerTime(PowerJobInstanceInfoEntity entity) {
        if (entity.getActualTriggerTime() != null && entity.getActualTriggerTime() > 0) {
            return entity.getActualTriggerTime();
        }
        if (entity.getExpectedTriggerTime() != null && entity.getExpectedTriggerTime() > 0) {
            return entity.getExpectedTriggerTime();
        }
        if (entity.getGmtCreate() != null) {
            return entity.getGmtCreate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        return null;
    }

    private Long duration(InstanceInfoDTO info) {
        if (info.getActualTriggerTime() == null || info.getFinishedTime() == null) {
            return null;
        }
        long duration = info.getFinishedTime() - info.getActualTriggerTime();
        return duration >= 0 ? duration : null;
    }

    private Long duration(PowerJobInstanceInfoEntity entity) {
        if (entity.getActualTriggerTime() == null || entity.getFinishedTime() == null) {
            return null;
        }
        long duration = entity.getFinishedTime() - entity.getActualTriggerTime();
        return duration >= 0 ? duration : null;
    }

    private String errorSummary(InstanceInfoDTO info) {
        String status = toMangoInstanceStatus(info.getStatus());
        if (JobInstanceStatus.FAILED.name().equals(status) || JobInstanceStatus.CANCELED.name().equals(status)) {
            return StringUtils.hasText(info.getResult()) ? info.getResult() : status;
        }
        return null;
    }

    private String errorSummary(PowerJobInstanceInfoEntity entity) {
        String status = toMangoInstanceStatus(entity.getStatus());
        if (JobInstanceStatus.FAILED.name().equals(status) || JobInstanceStatus.CANCELED.name().equals(status)) {
            return StringUtils.hasText(entity.getResult()) ? entity.getResult() : status;
        }
        return null;
    }
}
