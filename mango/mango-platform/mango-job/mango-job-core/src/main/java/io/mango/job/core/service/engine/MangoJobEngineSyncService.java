package io.mango.job.core.service.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.job.api.enums.JobInstanceStatus;
import io.mango.job.api.enums.JobSyncStatus;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobEngineMappingEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.job.core.mapper.MangoJobEngineMappingMapper;
import io.mango.job.core.mapper.MangoJobInstanceMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 默认 Mango Job 引擎同步服务。
 */
@Service
public class MangoJobEngineSyncService implements IMangoJobEngineSyncService {

    private final IMangoJobEngineRegistry engineRegistry;

    private final MangoJobDefinitionMapper definitionMapper;

    private final MangoJobInstanceMapper instanceMapper;

    private final MangoJobEngineMappingMapper mappingMapper;

    public MangoJobEngineSyncService(IMangoJobEngineRegistry engineRegistry,
                                     MangoJobDefinitionMapper definitionMapper,
                                     MangoJobInstanceMapper instanceMapper,
                                     MangoJobEngineMappingMapper mappingMapper) {
        this.engineRegistry = engineRegistry;
        this.definitionMapper = definitionMapper;
        this.instanceMapper = instanceMapper;
        this.mappingMapper = mappingMapper;
    }

    @Override
    public void syncDefinition(MangoJobDefinitionEntity definition, String action) {
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
            }
        });
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
        instanceMapper.updateById(instance);
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

    private static String resolve(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }
}
