package io.mango.job.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.job.api.enums.JobDefinitionStatus;
import io.mango.job.api.enums.JobSyncStatus;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Job 任务定义资源处理器。
 */
@Component
@RequiredArgsConstructor
public class MangoJobDefinitionResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "mango_job_definition";
    private static final String DEFAULT_TENANT_ID = "1";
    private static final String DEFAULT_JOB_TYPE = "BUILTIN";
    private static final String DEFAULT_MISFIRE_STRATEGY = "IGNORE";
    private static final String DEFAULT_CONCURRENCY_POLICY = "SERIAL";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    private static final String DEFAULT_ENGINE_TYPE = "MANGO_NATIVE";
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;
    private static final int DEFAULT_MAX_RETRY_COUNT = 0;
    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;

    private final MangoJobDefinitionMapper definitionMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.JOB_DEFINITION;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("appCode")
                .requiredField("jobCode")
                .requiredField("jobName")
                .requiredField("scheduleType")
                .requiredField("handlerName")
                .fieldDescription("jobId", "任务定义稳定 ID，可选；不填时使用资源 ID。")
                .fieldDescription("tenantId", "租户 ID，默认 1。")
                .fieldDescription("appCode", "所属逻辑应用。")
                .fieldDescription("ownerService", "任务归属服务，默认跟随 appCode。")
                .fieldDescription("workerGroup", "Worker 分组，默认跟随 ownerService。")
                .fieldDescription("moduleCode", "来源模块编码，默认使用资源 moduleCode。")
                .fieldDescription("jobCode", "任务编码，租户和应用内唯一。")
                .fieldDescription("jobName", "任务名称。")
                .fieldDescription("jobType", "任务类型，默认 BUILTIN。")
                .fieldDescription("scheduleType", "调度类型：CRON、FIXED_RATE、ONE_TIME、MANUAL。")
                .fieldDescription("scheduleExpression", "调度表达式，MANUAL 可为空。")
                .fieldDescription("handlerName", "Spring Bean 处理器名称。")
                .fieldDescription("handlerVersion", "处理器版本。")
                .fieldDescription("paramSchema", "参数表单 JSON Schema。")
                .fieldDescription("paramValue", "默认参数 JSON。")
                .fieldDescription("misfireStrategy", "错过触发策略，默认 IGNORE。")
                .fieldDescription("concurrencyPolicy", "并发策略，默认 SERIAL。")
                .fieldDescription("timeoutSeconds", "执行超时秒数，默认 300。")
                .fieldDescription("retryPolicy", "重试策略 JSON。")
                .fieldDescription("timezone", "调度时区，默认 Asia/Shanghai。")
                .fieldDescription("maxRetryCount", "最大重试次数，默认 0。")
                .fieldDescription("status", "初始状态，默认 DISABLED；已有非 DRAFT 状态不会被资源覆盖。")
                .fieldDescription("engineType", "调度引擎类型，默认 MANGO_NATIVE。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        JobDefinitionPayload payload = JobDefinitionPayload.from(resource);
        MangoJobDefinitionEntity entity = resolveByStableId(resource, payload.jobId());
        if (entity == null) {
            entity = find(payload.tenantId(), payload.appCode(), payload.jobCode());
        }
        if (entity == null) {
            entity = new MangoJobDefinitionEntity();
            entity.setId(payload.jobId());
            entity.setTenantId(payload.tenantId());
            entity.setAppCode(payload.appCode());
            entity.setJobCode(payload.jobCode());
            apply(entity, payload, true);
            definitionMapper.insert(entity);
        } else {
            apply(entity, payload, false);
            definitionMapper.updateById(entity);
        }
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Job definition synced: " + payload.jobCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        MangoJobDefinitionEntity entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Job definition not found");
        }
        entity.setDeleted(NOT_DELETED);
        entity.setStatus(JobDefinitionStatus.DISABLED.name());
        entity.setSyncStatus(resolveSyncStatus(entity));
        entity.setUpdatedAt(LocalDateTime.now());
        definitionMapper.updateById(entity);
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Job definition disabled: " + entity.getJobCode());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        MangoJobDefinitionEntity entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Job definition not found");
        }
        entity.setDeleted(DELETED);
        entity.setStatus(JobDefinitionStatus.DISABLED.name());
        entity.setSyncStatus(resolveSyncStatus(entity));
        entity.setUpdatedAt(LocalDateTime.now());
        definitionMapper.updateById(entity);
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Job definition deleted: " + entity.getJobCode());
    }

    private void apply(MangoJobDefinitionEntity entity, JobDefinitionPayload payload, boolean create) {
        LocalDateTime now = LocalDateTime.now();
        entity.setTenantId(payload.tenantId());
        entity.setAppCode(payload.appCode());
        entity.setOwnerService(payload.ownerService());
        entity.setWorkerGroup(payload.workerGroup());
        entity.setModuleCode(payload.moduleCode());
        entity.setJobCode(payload.jobCode());
        entity.setJobName(payload.jobName());
        entity.setJobType(payload.jobType());
        entity.setScheduleType(payload.scheduleType());
        entity.setScheduleExpression(payload.scheduleExpression());
        entity.setHandlerName(payload.handlerName());
        entity.setHandlerVersion(payload.handlerVersion());
        entity.setParamSchema(payload.paramSchema());
        entity.setParamValue(payload.paramValue());
        entity.setMisfireStrategy(payload.misfireStrategy());
        entity.setConcurrencyPolicy(payload.concurrencyPolicy());
        entity.setTimeoutSeconds(payload.timeoutSeconds());
        entity.setRetryPolicy(payload.retryPolicy());
        entity.setTimezone(payload.timezone());
        entity.setMaxRetryCount(payload.maxRetryCount());
        entity.setVersion(payload.definitionVersion());
        entity.setDeleted(NOT_DELETED);
        entity.setEngineType(payload.engineType());
        entity.setSyncError(null);
        entity.setSyncStatus(resolveSyncStatus(entity));
        if (create || JobDefinitionStatus.DRAFT.name().equals(entity.getStatus())) {
            entity.setStatus(payload.status());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy(0L);
        }
        entity.setUpdatedAt(now);
        if (entity.getUpdatedBy() == null) {
            entity.setUpdatedBy(0L);
        }
    }

    private MangoJobDefinitionEntity resolve(ResourceDeclaration resource) {
        String tenantId = defaultText(fieldText(resource, "tenantId", false), DEFAULT_TENANT_ID);
        String appCode = fieldText(resource, "appCode", false);
        String jobCode = fieldText(resource, "jobCode", false);
        MangoJobDefinitionEntity entity = resolveByStableId(resource, null);
        if (entity != null) {
            return entity;
        }
        if (StringUtils.hasText(appCode) && StringUtils.hasText(jobCode)) {
            entity = find(tenantId, appCode.trim(), jobCode.trim());
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private MangoJobDefinitionEntity resolveByStableId(ResourceDeclaration resource, Long defaultJobId) {
        Long jobId = fieldLong(resource, "jobId", false, null);
        if (jobId != null) {
            return definitionMapper.selectById(jobId);
        }
        Long targetId = fieldLong(resource, "targetId", false, null);
        if (targetId != null) {
            return definitionMapper.selectById(targetId);
        }
        return defaultJobId == null ? null : definitionMapper.selectById(defaultJobId);
    }

    private MangoJobDefinitionEntity find(String tenantId, String appCode, String jobCode) {
        return definitionMapper.selectOne(new LambdaQueryWrapper<MangoJobDefinitionEntity>()
                .eq(MangoJobDefinitionEntity::getTenantId, tenantId)
                .eq(MangoJobDefinitionEntity::getAppCode, appCode)
                .eq(MangoJobDefinitionEntity::getJobCode, jobCode)
                .last("limit 1"));
    }

    private static String resolveSyncStatus(MangoJobDefinitionEntity entity) {
        if (StringUtils.hasText(entity.getEngineJobId())) {
            return StringUtils.hasText(entity.getSyncStatus())
                    ? entity.getSyncStatus()
                    : JobSyncStatus.SYNCED.name();
        }
        return JobSyncStatus.PENDING.name();
    }

    private record JobDefinitionPayload(Long jobId, String tenantId, String appCode, String ownerService,
                                        String workerGroup, String moduleCode, String jobCode, String jobName,
                                        String jobType, String scheduleType, String scheduleExpression,
                                        String handlerName, String handlerVersion, String paramSchema,
                                        String paramValue, String misfireStrategy, String concurrencyPolicy,
                                        Integer timeoutSeconds, String retryPolicy, String timezone,
                                        Integer maxRetryCount, Integer definitionVersion, String status,
                                        String engineType) {

        private static JobDefinitionPayload from(ResourceDeclaration resource) {
            String appCode = requiredText(resource, "appCode").trim();
            String ownerService = defaultText(fieldText(resource, "ownerService", false), appCode);
            String workerGroup = defaultText(fieldText(resource, "workerGroup", false), ownerService);
            return new JobDefinitionPayload(
                    fieldLong(resource, "jobId", false, Long.valueOf(resource.getId())),
                    defaultText(fieldText(resource, "tenantId", false), DEFAULT_TENANT_ID),
                    appCode,
                    ownerService,
                    workerGroup,
                    defaultText(fieldText(resource, "moduleCode", false), resource.getModuleCode()),
                    requiredText(resource, "jobCode").trim(),
                    requiredText(resource, "jobName").trim(),
                    defaultText(fieldText(resource, "jobType", false), DEFAULT_JOB_TYPE),
                    requiredText(resource, "scheduleType").trim(),
                    fieldText(resource, "scheduleExpression", false),
                    requiredText(resource, "handlerName").trim(),
                    fieldText(resource, "handlerVersion", false),
                    fieldText(resource, "paramSchema", false),
                    defaultText(fieldText(resource, "paramValue", false), "{}"),
                    defaultText(fieldText(resource, "misfireStrategy", false), DEFAULT_MISFIRE_STRATEGY),
                    defaultText(fieldText(resource, "concurrencyPolicy", false), DEFAULT_CONCURRENCY_POLICY),
                    fieldInt(resource, "timeoutSeconds", false, DEFAULT_TIMEOUT_SECONDS),
                    fieldText(resource, "retryPolicy", false),
                    defaultText(fieldText(resource, "timezone", false), DEFAULT_TIMEZONE),
                    fieldInt(resource, "maxRetryCount", false, DEFAULT_MAX_RETRY_COUNT),
                    fieldInt(resource, "definitionVersion", false, 0),
                    defaultText(fieldText(resource, "status", false), JobDefinitionStatus.DISABLED.name()),
                    defaultText(fieldText(resource, "engineType", false), DEFAULT_ENGINE_TYPE)
            );
        }
    }

    private static String requiredText(ResourceDeclaration resource, String name) {
        String text = fieldText(resource, name, true);
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException("JOB_DEFINITION field is required: " + name);
        }
        return text;
    }

    private static String fieldText(ResourceDeclaration resource, String name, boolean required) {
        return toText(fieldValue(resource, name, required));
    }

    private static Long fieldLong(ResourceDeclaration resource, String name, boolean required, Long defaultValue) {
        return toLong(fieldValue(resource, name, required), required, defaultValue);
    }

    private static Integer fieldInt(ResourceDeclaration resource, String name, boolean required, Integer defaultValue) {
        return toInt(fieldValue(resource, name, required), required, defaultValue);
    }

    private static Object fieldValue(ResourceDeclaration resource, String name, boolean required) {
        ResourceField field = resource.getFields().get(name);
        Object value = field == null ? null : field.getValue();
        if (required && value == null) {
            throw new IllegalStateException("JOB_DEFINITION field is required: " + name);
        }
        return value;
    }

    private static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private static String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long toLong(Object value, boolean required, Long defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("JOB_DEFINITION long value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static Integer toInt(Object value, boolean required, Integer defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("JOB_DEFINITION int value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }
}
