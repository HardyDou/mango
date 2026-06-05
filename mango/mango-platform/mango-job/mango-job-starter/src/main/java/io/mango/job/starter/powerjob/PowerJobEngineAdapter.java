package io.mango.job.starter.powerjob;

import io.mango.job.api.enums.JobDefinitionStatus;
import io.mango.job.api.enums.JobEngineType;
import io.mango.job.api.enums.JobScheduleType;
import io.mango.job.api.enums.JobType;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.service.engine.IMangoJobEngine;
import io.mango.job.core.service.engine.MangoJobEngineRequest;
import io.mango.job.core.service.engine.MangoJobEngineResult;
import io.mango.job.core.service.engine.MangoJobTriggerRequest;
import org.springframework.util.StringUtils;
import tech.powerjob.common.enums.DispatchStrategy;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.request.http.RunJobRequest;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.ResultDTO;

/**
 * PowerJob 引擎适配器。
 */
public class PowerJobEngineAdapter implements IMangoJobEngine {

    private final IPowerJobClientOperations client;

    private final PowerJobProperties properties;

    public PowerJobEngineAdapter(IPowerJobClientOperations client, PowerJobProperties properties) {
        this.client = client;
        this.properties = properties;
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
                    .setInstanceParams(definition.getParamValue())
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
        request.setJobParams(definition.getParamValue());
        request.setTimeExpressionType(timeExpressionType(definition.getScheduleType()));
        request.setTimeExpression(timeExpression(definition));
        request.setExecuteType(ExecuteType.STANDALONE);
        request.setProcessorType(processorType(definition.getJobType()));
        request.setProcessorInfo(definition.getHandlerName());
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
        JobType type = JobType.valueOf(jobType);
        return switch (type) {
            case BUILTIN, REMOTE_API, HTTP, ENGINE_NATIVE -> ProcessorType.BUILT_IN;
            case SCRIPT -> ProcessorType.SHELL;
        };
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
}
