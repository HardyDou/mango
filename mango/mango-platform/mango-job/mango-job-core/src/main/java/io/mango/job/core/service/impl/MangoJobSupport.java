package io.mango.job.core.service.impl;

import io.mango.common.result.Require;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.job.api.enums.JobDefinitionStatus;
import io.mango.job.api.enums.JobEngineType;
import io.mango.job.api.enums.JobHandleStatus;
import io.mango.job.api.enums.JobInstanceStatus;
import io.mango.job.api.enums.JobScheduleType;
import io.mango.job.api.enums.JobSyncStatus;
import io.mango.job.api.enums.JobTriggerType;
import io.mango.job.api.enums.JobType;
import io.mango.job.api.vo.MangoJobDefinitionVO;
import io.mango.job.api.vo.MangoJobEngineStatusVO;
import io.mango.job.api.vo.MangoJobInstanceVO;
import io.mango.job.api.vo.MangoJobLogIndexVO;
import io.mango.job.api.vo.MangoJobWorkerSnapshotVO;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import io.mango.job.core.entity.MangoJobLogIndexEntity;
import io.mango.job.core.entity.MangoJobWorkerSnapshotEntity;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;

/**
 * Job 服务内部支持方法。
 */
final class MangoJobSupport {

    private MangoJobSupport() {
    }

    static String currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, "缺少当前租户上下文");
        return tenantId;
    }

    static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    static boolean hasValidWorkerAddress(String value) {
        String address = trimToNull(value);
        if (address == null) {
            return false;
        }
        String normalized = address.toUpperCase(Locale.ROOT);
        return !("N/A".equals(normalized)
                || "UNKNOWN".equals(normalized)
                || "NULL".equals(normalized)
                || "-".equals(normalized));
    }

    static String normalizeRequired(String value, String message) {
        Require.notBlank(value, message);
        return value.trim();
    }

    static JobType jobType(String value) {
        return enumValue(JobType.class, value, "不支持的任务类型：" + value);
    }

    static JobScheduleType scheduleType(String value) {
        return enumValue(JobScheduleType.class, value, "不支持的调度类型：" + value);
    }

    static JobDefinitionStatus definitionStatus(String value) {
        return enumValue(JobDefinitionStatus.class, value, "不支持的任务状态：" + value);
    }

    static JobEngineType engineType(String value) {
        return enumValue(JobEngineType.class, value, "不支持的引擎类型：" + value);
    }

    static String syncStatus(JobSyncStatus status) {
        return status.name();
    }

    static String instanceStatus(JobInstanceStatus status) {
        return status.name();
    }

    static String triggerType(JobTriggerType type) {
        return type.name();
    }

    static String handleStatus(JobHandleStatus status) {
        return status.name();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> enumType, String value, String message) {
        Require.notBlank(value, message);
        try {
            return Enum.valueOf(enumType, value.trim());
        } catch (IllegalArgumentException ex) {
            return Require.fail(400, message + "，可选值：" + Arrays.toString(enumType.getEnumConstants()));
        }
    }

    static MangoJobDefinitionVO toDefinitionVO(MangoJobDefinitionEntity entity) {
        if (entity == null) {
            return null;
        }
        MangoJobDefinitionVO vo = new MangoJobDefinitionVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setAppCode(entity.getAppCode());
        vo.setOwnerService(entity.getOwnerService());
        vo.setWorkerGroup(entity.getWorkerGroup());
        vo.setJobCode(entity.getJobCode());
        vo.setJobName(entity.getJobName());
        vo.setJobType(entity.getJobType());
        vo.setScheduleType(entity.getScheduleType());
        vo.setScheduleExpression(entity.getScheduleExpression());
        vo.setHandlerName(entity.getHandlerName());
        vo.setParamSchema(entity.getParamSchema());
        vo.setParamValue(entity.getParamValue());
        vo.setMisfireStrategy(entity.getMisfireStrategy());
        vo.setConcurrencyPolicy(entity.getConcurrencyPolicy());
        vo.setTimeoutSeconds(entity.getTimeoutSeconds());
        vo.setRetryPolicy(entity.getRetryPolicy());
        vo.setStatus(entity.getStatus());
        vo.setEngineType(entity.getEngineType());
        vo.setEngineAppId(entity.getEngineAppId());
        vo.setEngineJobId(entity.getEngineJobId());
        vo.setSyncStatus(entity.getSyncStatus());
        vo.setSyncError(entity.getSyncError());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedBy(entity.getUpdatedBy());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    static MangoJobInstanceVO toInstanceVO(MangoJobInstanceEntity entity) {
        return toInstanceVO(entity, null);
    }

    static MangoJobInstanceVO toInstanceVO(MangoJobInstanceEntity entity, MangoJobDefinitionEntity definition) {
        if (entity == null) {
            return null;
        }
        MangoJobInstanceVO vo = new MangoJobInstanceVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setJobId(entity.getJobId());
        vo.setJobCode(entity.getJobCode());
        vo.setJobNameSnapshot(entity.getJobNameSnapshot());
        if (definition != null) {
            vo.setJobCode(definition.getJobCode());
            vo.setJobName(definition.getJobName());
        } else {
            vo.setJobName(entity.getJobNameSnapshot());
        }
        vo.setTriggerType(entity.getTriggerType());
        vo.setTriggerUserId(entity.getTriggerUserId());
        vo.setTriggerTime(entity.getTriggerTime());
        vo.setScheduledFireTime(entity.getScheduledFireTime());
        vo.setActualFireTime(entity.getActualFireTime());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setStatus(entity.getStatus());
        vo.setDurationMillis(entity.getDurationMillis());
        vo.setAttemptCount(entity.getAttemptCount());
        vo.setResultSummary(entity.getResultSummary());
        vo.setEngineType(entity.getEngineType());
        vo.setEngineInstanceId(entity.getEngineInstanceId());
        vo.setErrorSummary(entity.getErrorSummary());
        vo.setTraceId(entity.getTraceId());
        vo.setTriggerBatchNo(entity.getTriggerBatchNo());
        return vo;
    }

    static MangoJobLogIndexVO toLogVO(MangoJobLogIndexEntity entity) {
        if (entity == null) {
            return null;
        }
        MangoJobLogIndexVO vo = new MangoJobLogIndexVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setJobId(entity.getJobId());
        vo.setInstanceId(entity.getInstanceId());
        vo.setEngineType(entity.getEngineType());
        vo.setEngineInstanceId(entity.getEngineInstanceId());
        vo.setLogLocation(entity.getLogLocation());
        vo.setReadOffset(entity.getReadOffset());
        vo.setErrorSummary(entity.getErrorSummary());
        vo.setLastFetchedAt(entity.getLastFetchedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    static MangoJobWorkerSnapshotVO toWorkerVO(MangoJobWorkerSnapshotEntity entity) {
        if (entity == null) {
            return null;
        }
        MangoJobWorkerSnapshotVO vo = new MangoJobWorkerSnapshotVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setAppCode(entity.getAppCode());
        vo.setServiceCode(entity.getServiceCode());
        vo.setWorkerGroup(entity.getWorkerGroup());
        vo.setWorkerAddress(entity.getWorkerAddress());
        vo.setRuntimeAddress(entity.getRuntimeAddress());
        vo.setTransportType(entity.getTransportType());
        vo.setRegisterSource(entity.getRegisterSource());
        vo.setInstanceId(entity.getInstanceId());
        vo.setEngineType(entity.getEngineType());
        vo.setEngineWorkerId(entity.getEngineWorkerId());
        vo.setLastHeartbeatAt(entity.getLastHeartbeatAt());
        vo.setStatus(entity.getStatus());
        return vo;
    }

    static MangoJobEngineStatusVO emptyEngineStatus(JobEngineType engineType) {
        MangoJobEngineStatusVO vo = new MangoJobEngineStatusVO();
        vo.setEngineType(engineType.name());
        vo.setPendingCount(0L);
        vo.setFailedCount(0L);
        vo.setSyncedCount(0L);
        return vo;
    }
}
