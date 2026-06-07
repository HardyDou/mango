package io.mango.job.core.service.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.result.Require;
import io.mango.job.api.enums.JobEngineType;
import io.mango.job.api.enums.JobTriggerType;
import io.mango.job.api.enums.JobWorkerStatus;
import io.mango.job.api.enums.JobInstanceStatus;
import io.mango.job.api.enums.JobSyncStatus;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobEngineMappingEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import io.mango.job.core.entity.MangoJobLogIndexEntity;
import io.mango.job.core.entity.MangoJobWorkerSnapshotEntity;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.job.core.mapper.MangoJobEngineMappingMapper;
import io.mango.job.core.mapper.MangoJobInstanceMapper;
import io.mango.job.core.mapper.MangoJobLogIndexMapper;
import io.mango.job.core.mapper.MangoJobWorkerSnapshotMapper;
import io.mango.job.core.service.nativeengine.IMangoNativeJobRuntime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 默认 Mango Job 引擎同步服务。
 */
@Service
public class MangoJobEngineSyncService implements IMangoJobEngineSyncService {

    private final IMangoJobEngineRegistry engineRegistry;

    private final MangoJobDefinitionMapper definitionMapper;

    private final MangoJobInstanceMapper instanceMapper;

    private final MangoJobEngineMappingMapper mappingMapper;

    private final MangoJobLogIndexMapper logIndexMapper;

    private final MangoJobWorkerSnapshotMapper workerSnapshotMapper;

    private final IMangoNativeJobRuntime nativeJobRuntime;

    public MangoJobEngineSyncService(IMangoJobEngineRegistry engineRegistry,
                                     MangoJobDefinitionMapper definitionMapper,
                                     MangoJobInstanceMapper instanceMapper,
                                     MangoJobEngineMappingMapper mappingMapper,
                                     MangoJobLogIndexMapper logIndexMapper,
                                     MangoJobWorkerSnapshotMapper workerSnapshotMapper) {
        this(engineRegistry, definitionMapper, instanceMapper, mappingMapper, logIndexMapper, workerSnapshotMapper, null);
    }

    @Autowired
    public MangoJobEngineSyncService(IMangoJobEngineRegistry engineRegistry,
                                     MangoJobDefinitionMapper definitionMapper,
                                     MangoJobInstanceMapper instanceMapper,
                                     MangoJobEngineMappingMapper mappingMapper,
                                     MangoJobLogIndexMapper logIndexMapper,
                                     MangoJobWorkerSnapshotMapper workerSnapshotMapper,
                                     IMangoNativeJobRuntime nativeJobRuntime) {
        this.engineRegistry = engineRegistry;
        this.definitionMapper = definitionMapper;
        this.instanceMapper = instanceMapper;
        this.mappingMapper = mappingMapper;
        this.logIndexMapper = logIndexMapper;
        this.workerSnapshotMapper = workerSnapshotMapper;
        this.nativeJobRuntime = nativeJobRuntime;
    }

    @Override
    public void syncDefinition(MangoJobDefinitionEntity definition, String action) {
        if (isNative(definition)) {
            nativeJobRuntime.syncDefinition(definition);
            upsertJobMapping(definition, JobSyncStatus.SYNCED.name(), null);
            return;
        }
        engineRegistry.findEngine(definition.getEngineType()).ifPresentOrElse(engine -> {
            MangoJobEngineRequest request = new MangoJobEngineRequest();
            request.setDefinition(definition);
            request.setAction(action);
            MangoJobEngineResult result = engine.syncDefinition(request);
            applyDefinitionSyncResult(definition, result);
        }, () -> markDefinitionPending(definition));
    }

    @Override
    public void deleteDefinition(MangoJobDefinitionEntity definition) {
        if (isNative(definition)) {
            nativeJobRuntime.deleteDefinition(definition);
            return;
        }
        engineRegistry.findEngine(definition.getEngineType()).ifPresent(engine -> {
            MangoJobEngineRequest request = new MangoJobEngineRequest();
            request.setDefinition(definition);
            request.setAction("DELETE");
            MangoJobEngineResult result = engine.deleteDefinition(request);
            Require.isTrue(result.isSuccess(), "删除引擎任务失败：" + result.getErrorSummary());
        });
    }

    @Override
    public void trigger(MangoJobDefinitionEntity definition, MangoJobInstanceEntity instance, String batchNo) {
        if (isNative(definition)) {
            nativeJobRuntime.trigger(definition, instance, batchNo, null);
            insertInstanceMapping(definition, instance, JobSyncStatus.SYNCED.name(), null);
            return;
        }
        engineRegistry.findEngine(definition.getEngineType()).ifPresent(engine -> {
            MangoJobTriggerRequest request = new MangoJobTriggerRequest();
            request.setDefinition(definition);
            request.setInstance(instance);
            request.setBatchNo(batchNo);
            MangoJobEngineResult result = engine.trigger(request);
            applyTriggerResult(definition, instance, result);
        });
    }

    @Override
    public void trigger(MangoJobDefinitionEntity definition,
                        MangoJobInstanceEntity instance,
                        String batchNo,
                        String paramValue) {
        if (isNative(definition)) {
            nativeJobRuntime.trigger(definition, instance, batchNo, paramValue);
            insertInstanceMapping(definition, instance, JobSyncStatus.SYNCED.name(), null);
            return;
        }
        engineRegistry.findEngine(definition.getEngineType()).ifPresent(engine -> {
            MangoJobTriggerRequest request = new MangoJobTriggerRequest();
            request.setDefinition(definition);
            request.setInstance(instance);
            request.setBatchNo(batchNo);
            request.setParamValue(paramValue);
            MangoJobEngineResult result = engine.trigger(request);
            applyTriggerResult(definition, instance, result);
        });
    }

    @Override
    public void refreshInstance(MangoJobDefinitionEntity definition, MangoJobInstanceEntity instance) {
        if (isNative(definition)) {
            return;
        }
        if (!StringUtils.hasText(instance.getEngineInstanceId())) {
            return;
        }
        engineRegistry.findEngine(definition.getEngineType()).ifPresent(engine -> {
            MangoJobTriggerRequest request = new MangoJobTriggerRequest();
            request.setDefinition(definition);
            request.setInstance(instance);
            request.setBatchNo(instance.getTriggerBatchNo());
            MangoJobEngineResult result = engine.refreshInstance(request);
            if (result.isSuccess()) {
                applyInstanceRefreshResult(instance, result);
                upsertWorkerSnapshot(definition, result);
            }
        });
    }

    @Override
    public void importScheduledInstances(MangoJobDefinitionEntity definition,
                                         LocalDateTime triggerTimeStart,
                                         LocalDateTime triggerTimeEnd,
                                         int limit) {
        if (isNative(definition)) {
            nativeJobRuntime.importScheduledInstances(definition, triggerTimeStart, triggerTimeEnd, limit);
            return;
        }
        if (!StringUtils.hasText(definition.getEngineJobId())) {
            return;
        }
        engineRegistry.findEngine(definition.getEngineType()).ifPresent(engine -> {
            MangoJobInstanceImportRequest request = new MangoJobInstanceImportRequest();
            request.setDefinition(definition);
            request.setTriggerTimeStart(triggerTimeStart);
            request.setTriggerTimeEnd(triggerTimeEnd);
            request.setLimit(limit);
            engine.importInstances(request).stream()
                    .filter(MangoJobEngineInstanceSnapshot::hasEngineInstanceId)
                    .forEach(snapshot -> importScheduledInstance(definition, snapshot));
        });
    }

    private void importScheduledInstance(MangoJobDefinitionEntity definition,
                                         MangoJobEngineInstanceSnapshot snapshot) {
        MangoJobInstanceEntity instance = selectInstanceByEngineId(definition, snapshot.getEngineInstanceId());
        if (instance == null) {
            instance = new MangoJobInstanceEntity();
            instance.setTenantId(definition.getTenantId());
            instance.setJobId(definition.getId());
            instance.setTriggerType(JobTriggerType.SCHEDULED.name());
            instance.setTriggerTime(resolveTime(snapshot.getTriggerTime()));
            instance.setStatus(resolveStatus(snapshot.getStatus()));
            instance.setEngineType(definition.getEngineType());
            instance.setEngineInstanceId(snapshot.getEngineInstanceId());
            instance.setTraceId(snapshot.getEngineInstanceId());
            instance.setTriggerBatchNo(snapshot.getTriggerBatchNo());
            applySnapshot(instance, snapshot);
            instanceMapper.insert(instance);
        } else {
            applySnapshot(instance, snapshot);
            instanceMapper.updateById(instance);
        }
        upsertLogIndex(definition, instance);
        upsertInstanceMapping(definition, instance, JobSyncStatus.SYNCED.name(), null);
        upsertWorkerSnapshot(definition, snapshot);
    }

    private MangoJobInstanceEntity selectInstanceByEngineId(MangoJobDefinitionEntity definition, String engineInstanceId) {
        return instanceMapper.selectOne(new LambdaQueryWrapper<MangoJobInstanceEntity>()
                .eq(MangoJobInstanceEntity::getTenantId, definition.getTenantId())
                .eq(MangoJobInstanceEntity::getEngineType, definition.getEngineType())
                .eq(MangoJobInstanceEntity::getEngineInstanceId, engineInstanceId)
                .last("limit 1"));
    }

    private void applySnapshot(MangoJobInstanceEntity instance, MangoJobEngineInstanceSnapshot snapshot) {
        if (snapshot.getTriggerTime() != null) {
            instance.setTriggerTime(snapshot.getTriggerTime());
        }
        if (snapshot.getStartTime() != null) {
            instance.setStartTime(snapshot.getStartTime());
        }
        if (snapshot.getEndTime() != null) {
            instance.setEndTime(snapshot.getEndTime());
        }
        if (StringUtils.hasText(snapshot.getStatus())) {
            instance.setStatus(snapshot.getStatus());
        }
        if (snapshot.getDurationMillis() != null) {
            instance.setDurationMillis(snapshot.getDurationMillis());
        }
        if (StringUtils.hasText(snapshot.getErrorSummary())) {
            instance.setErrorSummary(snapshot.getErrorSummary());
        } else if (JobInstanceStatus.SUCCESS.name().equals(instance.getStatus())) {
            instance.setErrorSummary(null);
        }
        if (StringUtils.hasText(snapshot.getTriggerBatchNo())) {
            instance.setTriggerBatchNo(snapshot.getTriggerBatchNo());
        }
    }

    private void applyDefinitionSyncResult(MangoJobDefinitionEntity definition, MangoJobEngineResult result) {
        if (result.isSuccess()) {
            definition.setEngineAppId(resolve(result.getEngineAppId(), definition.getEngineAppId()));
            definition.setEngineJobId(resolve(result.getEngineJobId(), definition.getEngineJobId()));
            definition.setSyncStatus(JobSyncStatus.SYNCED.name());
            definition.setSyncError(null);
            definitionMapper.updateById(definition);
            upsertJobMapping(definition, JobSyncStatus.SYNCED.name(), null);
            return;
        }
        definition.setSyncStatus(JobSyncStatus.FAILED.name());
        definition.setSyncError(result.getErrorSummary());
        definitionMapper.updateById(definition);
        upsertJobMapping(definition, JobSyncStatus.FAILED.name(), result.getErrorSummary());
    }

    private void applyTriggerResult(MangoJobDefinitionEntity definition,
                                    MangoJobInstanceEntity instance,
                                    MangoJobEngineResult result) {
        if (result.isSuccess()) {
            instance.setEngineInstanceId(result.getEngineInstanceId());
            instanceMapper.updateById(instance);
            insertInstanceMapping(definition, instance, JobSyncStatus.SYNCED.name(), null);
            refreshInstance(definition, instance);
            return;
        }
        instance.setErrorSummary(result.getErrorSummary());
        instance.setStatus(JobInstanceStatus.FAILED.name());
        instance.setEndTime(LocalDateTime.now());
        instanceMapper.updateById(instance);
        upsertLogIndex(definition, instance);
        insertInstanceMapping(definition, instance, JobSyncStatus.FAILED.name(), result.getErrorSummary());
        Require.fail(500, "触发引擎任务失败：" + result.getErrorSummary());
    }

    private void applyInstanceRefreshResult(MangoJobInstanceEntity instance, MangoJobEngineResult result) {
        if (StringUtils.hasText(result.getInstanceStatus())) {
            instance.setStatus(result.getInstanceStatus());
        }
        if (result.getStartTime() != null) {
            instance.setStartTime(result.getStartTime());
        }
        if (result.getEndTime() != null) {
            instance.setEndTime(result.getEndTime());
        }
        if (result.getDurationMillis() != null) {
            instance.setDurationMillis(result.getDurationMillis());
        }
        if (StringUtils.hasText(result.getErrorSummary())) {
            instance.setErrorSummary(result.getErrorSummary());
        } else if (JobInstanceStatus.SUCCESS.name().equals(instance.getStatus())) {
            instance.setErrorSummary(null);
        }
        instanceMapper.updateById(instance);
    }

    private void upsertWorkerSnapshot(MangoJobDefinitionEntity definition, MangoJobEngineResult result) {
        if (!hasValidWorkerAddress(result.getWorkerAddress())) {
            return;
        }
        MangoJobWorkerSnapshotEntity snapshot = new MangoJobWorkerSnapshotEntity();
        snapshot.setTenantId(definition.getTenantId());
        snapshot.setAppCode(definition.getAppCode());
        snapshot.setEngineType(definition.getEngineType());
        snapshot.setWorkerAddress(result.getWorkerAddress());
        snapshot.setEngineWorkerId(result.getWorkerAddress());
        snapshot.setStatus(JobWorkerStatus.ONLINE.name());
        snapshot.setLastHeartbeatAt(LocalDateTime.now());
        upsertWorkerSnapshot(snapshot);
    }

    private void upsertWorkerSnapshot(MangoJobDefinitionEntity definition, MangoJobEngineInstanceSnapshot snapshot) {
        if (!hasValidWorkerAddress(snapshot.getWorkerAddress())) {
            return;
        }
        MangoJobWorkerSnapshotEntity worker = new MangoJobWorkerSnapshotEntity();
        worker.setTenantId(definition.getTenantId());
        worker.setAppCode(definition.getAppCode());
        worker.setEngineType(definition.getEngineType());
        worker.setWorkerAddress(snapshot.getWorkerAddress());
        worker.setEngineWorkerId(snapshot.getWorkerAddress());
        worker.setStatus(JobWorkerStatus.ONLINE.name());
        worker.setLastHeartbeatAt(LocalDateTime.now());
        upsertWorkerSnapshot(worker);
    }

    private void upsertWorkerSnapshot(MangoJobWorkerSnapshotEntity snapshot) {
        MangoJobWorkerSnapshotEntity existing = workerSnapshotMapper.selectOne(
                new LambdaQueryWrapper<MangoJobWorkerSnapshotEntity>()
                        .eq(MangoJobWorkerSnapshotEntity::getTenantId, snapshot.getTenantId())
                        .eq(MangoJobWorkerSnapshotEntity::getAppCode, snapshot.getAppCode())
                        .eq(MangoJobWorkerSnapshotEntity::getEngineType, snapshot.getEngineType())
                        .eq(MangoJobWorkerSnapshotEntity::getWorkerAddress, snapshot.getWorkerAddress())
                        .last("limit 1"));
        if (existing == null) {
            workerSnapshotMapper.insert(snapshot);
            return;
        }
        existing.setStatus(snapshot.getStatus());
        existing.setEngineWorkerId(snapshot.getEngineWorkerId());
        existing.setLastHeartbeatAt(snapshot.getLastHeartbeatAt());
        workerSnapshotMapper.updateById(existing);
    }

    private void markDefinitionPending(MangoJobDefinitionEntity definition) {
        definition.setSyncStatus(JobSyncStatus.PENDING.name());
        definitionMapper.updateById(definition);
    }

    private void upsertJobMapping(MangoJobDefinitionEntity definition, String syncStatus, String syncError) {
        MangoJobEngineMappingEntity mapping = mappingMapper.selectOne(
                new LambdaQueryWrapper<MangoJobEngineMappingEntity>()
                        .eq(MangoJobEngineMappingEntity::getTenantId, definition.getTenantId())
                        .eq(MangoJobEngineMappingEntity::getJobId, definition.getId())
                        .isNull(MangoJobEngineMappingEntity::getInstanceId));
        if (mapping == null) {
            mapping = new MangoJobEngineMappingEntity();
            mapping.setTenantId(definition.getTenantId());
            mapping.setJobId(definition.getId());
            mapping.setAppCode(definition.getAppCode());
        }
        mapping.setEngineType(definition.getEngineType());
        mapping.setEngineAppId(definition.getEngineAppId());
        mapping.setEngineJobId(definition.getEngineJobId());
        mapping.setSyncStatus(syncStatus);
        mapping.setSyncError(syncError);
        if (mapping.getId() == null) {
            mappingMapper.insert(mapping);
            return;
        }
        mappingMapper.updateById(mapping);
    }

    private void insertInstanceMapping(MangoJobDefinitionEntity definition,
                                       MangoJobInstanceEntity instance,
                                       String syncStatus,
                                       String syncError) {
        MangoJobEngineMappingEntity mapping = new MangoJobEngineMappingEntity();
        mapping.setTenantId(definition.getTenantId());
        mapping.setJobId(definition.getId());
        mapping.setInstanceId(instance.getId());
        mapping.setAppCode(definition.getAppCode());
        mapping.setEngineType(definition.getEngineType());
        mapping.setEngineAppId(definition.getEngineAppId());
        mapping.setEngineJobId(definition.getEngineJobId());
        mapping.setEngineInstanceId(instance.getEngineInstanceId());
        mapping.setSyncStatus(syncStatus);
        mapping.setSyncError(syncError);
        mappingMapper.insert(mapping);
    }

    private void upsertInstanceMapping(MangoJobDefinitionEntity definition,
                                       MangoJobInstanceEntity instance,
                                       String syncStatus,
                                       String syncError) {
        MangoJobEngineMappingEntity mapping = mappingMapper.selectOne(
                new LambdaQueryWrapper<MangoJobEngineMappingEntity>()
                        .eq(MangoJobEngineMappingEntity::getTenantId, definition.getTenantId())
                        .eq(MangoJobEngineMappingEntity::getEngineType, definition.getEngineType())
                        .eq(MangoJobEngineMappingEntity::getEngineInstanceId, instance.getEngineInstanceId())
                        .last("limit 1"));
        if (mapping == null) {
            mapping = new MangoJobEngineMappingEntity();
            mapping.setTenantId(definition.getTenantId());
            mapping.setJobId(definition.getId());
            mapping.setInstanceId(instance.getId());
            mapping.setAppCode(definition.getAppCode());
            mapping.setEngineType(definition.getEngineType());
            mapping.setEngineAppId(definition.getEngineAppId());
            mapping.setEngineJobId(definition.getEngineJobId());
            mapping.setEngineInstanceId(instance.getEngineInstanceId());
            mapping.setSyncStatus(syncStatus);
            mapping.setSyncError(syncError);
            mappingMapper.insert(mapping);
            return;
        }
        mapping.setJobId(definition.getId());
        mapping.setInstanceId(instance.getId());
        mapping.setAppCode(definition.getAppCode());
        mapping.setEngineAppId(definition.getEngineAppId());
        mapping.setEngineJobId(definition.getEngineJobId());
        mapping.setSyncStatus(syncStatus);
        mapping.setSyncError(syncError);
        mappingMapper.updateById(mapping);
    }

    private void upsertLogIndex(MangoJobDefinitionEntity definition, MangoJobInstanceEntity instance) {
        MangoJobLogIndexEntity log = logIndexMapper.selectOne(new LambdaQueryWrapper<MangoJobLogIndexEntity>()
                .eq(MangoJobLogIndexEntity::getTenantId, definition.getTenantId())
                .eq(MangoJobLogIndexEntity::getInstanceId, instance.getId())
                .last("limit 1"));
        if (log == null) {
            log = new MangoJobLogIndexEntity();
            log.setId(IdWorker.getId());
            log.setTenantId(definition.getTenantId());
            log.setJobId(definition.getId());
            log.setInstanceId(instance.getId());
            log.setEngineType(definition.getEngineType());
            log.setEngineInstanceId(instance.getEngineInstanceId());
            log.setLogLocation("mango-job://jobs/" + definition.getId() + "/instances/" + instance.getId());
            log.setReadOffset(0L);
            log.setErrorSummary(instance.getErrorSummary());
            log.setLastFetchedAt(LocalDateTime.now());
            logIndexMapper.insert(log);
            return;
        }
        log.setEngineInstanceId(instance.getEngineInstanceId());
        log.setErrorSummary(instance.getErrorSummary());
        log.setLastFetchedAt(LocalDateTime.now());
        logIndexMapper.updateById(log);
    }

    private LocalDateTime resolveTime(LocalDateTime time) {
        return time == null ? LocalDateTime.now() : time;
    }

    private String resolveStatus(String status) {
        return StringUtils.hasText(status) ? status : JobInstanceStatus.WAITING.name();
    }

    private static String resolve(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private static boolean hasValidWorkerAddress(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return !("N/A".equals(normalized)
                || "UNKNOWN".equals(normalized)
                || "NULL".equals(normalized)
                || "-".equals(normalized));
    }

    private boolean isNative(MangoJobDefinitionEntity definition) {
        return definition != null
                && nativeJobRuntime != null
                && JobEngineType.MANGO_NATIVE.name().equals(definition.getEngineType());
    }
}
