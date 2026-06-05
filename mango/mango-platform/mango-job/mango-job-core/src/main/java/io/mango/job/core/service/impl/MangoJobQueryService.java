package io.mango.job.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.vo.PageResult;
import io.mango.job.api.enums.JobEngineType;
import io.mango.job.api.enums.JobInstanceStatus;
import io.mango.job.api.enums.JobSyncStatus;
import io.mango.job.api.query.MangoJobInstancePageQuery;
import io.mango.job.api.query.MangoJobLogPageQuery;
import io.mango.job.api.query.MangoJobWorkerPageQuery;
import io.mango.job.api.vo.MangoJobEngineStatusVO;
import io.mango.job.api.vo.MangoJobHandlerVO;
import io.mango.job.api.vo.MangoJobInstanceVO;
import io.mango.job.api.vo.MangoJobLogIndexVO;
import io.mango.job.api.vo.MangoJobWorkerSnapshotVO;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import io.mango.job.core.entity.MangoJobLogIndexEntity;
import io.mango.job.core.entity.MangoJobWorkerSnapshotEntity;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.job.core.mapper.MangoJobInstanceMapper;
import io.mango.job.core.mapper.MangoJobLogIndexMapper;
import io.mango.job.core.mapper.MangoJobWorkerSnapshotMapper;
import io.mango.job.core.service.IMangoJobHandlerRegistry;
import io.mango.job.core.service.IMangoJobQueryService;
import io.mango.job.core.service.engine.IMangoJobEngineSyncService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Job 运行态查询服务实现。
 */
@Service
public class MangoJobQueryService implements IMangoJobQueryService {

    private final MangoJobInstanceMapper instanceMapper;

    private final MangoJobLogIndexMapper logIndexMapper;

    private final MangoJobWorkerSnapshotMapper workerSnapshotMapper;

    private final MangoJobDefinitionMapper definitionMapper;

    private final IMangoJobHandlerRegistry handlerRegistry;

    private final MangoJobDataSourceRouter dataSourceRouter;

    private final IMangoJobEngineSyncService engineSyncService;

    public MangoJobQueryService(MangoJobInstanceMapper instanceMapper,
                                MangoJobLogIndexMapper logIndexMapper,
                                MangoJobWorkerSnapshotMapper workerSnapshotMapper,
                                MangoJobDefinitionMapper definitionMapper,
                                IMangoJobHandlerRegistry handlerRegistry,
                                MangoJobDataSourceRouter dataSourceRouter,
                                IMangoJobEngineSyncService engineSyncService) {
        this.instanceMapper = instanceMapper;
        this.logIndexMapper = logIndexMapper;
        this.workerSnapshotMapper = workerSnapshotMapper;
        this.definitionMapper = definitionMapper;
        this.handlerRegistry = handlerRegistry;
        this.dataSourceRouter = dataSourceRouter;
        this.engineSyncService = engineSyncService;
    }

    @Override
    public PageResult<MangoJobInstanceVO> pageInstances(MangoJobInstancePageQuery query) {
        return dataSourceRouter.route(() -> {
            MangoJobInstancePageQuery resolved = query == null ? new MangoJobInstancePageQuery() : query;
            IPage<MangoJobInstanceEntity> page = instanceMapper.selectPage(
                    new Page<>(resolved.getPage(), resolved.getSize()),
                    instanceWrapper(resolved));
            refreshRunningInstances(page.getRecords());
            return PageResult.of(page.getRecords().stream().map(MangoJobSupport::toInstanceVO).toList(),
                    page.getTotal(), page.getCurrent(), page.getSize());
        });
    }

    @Override
    public PageResult<MangoJobLogIndexVO> pageLogs(MangoJobLogPageQuery query) {
        return dataSourceRouter.route(() -> {
            MangoJobLogPageQuery resolved = query == null ? new MangoJobLogPageQuery() : query;
            IPage<MangoJobLogIndexEntity> page = logIndexMapper.selectPage(
                    new Page<>(resolved.getPage(), resolved.getSize()),
                    logWrapper(resolved));
            return PageResult.of(page.getRecords().stream().map(MangoJobSupport::toLogVO).toList(),
                    page.getTotal(), page.getCurrent(), page.getSize());
        });
    }

    @Override
    public PageResult<MangoJobWorkerSnapshotVO> pageWorkers(MangoJobWorkerPageQuery query) {
        return dataSourceRouter.route(() -> {
            MangoJobWorkerPageQuery resolved = query == null ? new MangoJobWorkerPageQuery() : query;
            IPage<MangoJobWorkerSnapshotEntity> page = workerSnapshotMapper.selectPage(
                    new Page<>(resolved.getPage(), resolved.getSize()),
                    workerWrapper(resolved));
            return PageResult.of(page.getRecords().stream().map(MangoJobSupport::toWorkerVO).toList(),
                    page.getTotal(), page.getCurrent(), page.getSize());
        });
    }

    @Override
    public List<MangoJobHandlerVO> listHandlers() {
        return handlerRegistry.listHandlers();
    }

    @Override
    public List<MangoJobEngineStatusVO> listEngineStatus() {
        return dataSourceRouter.route(() -> Arrays.stream(JobEngineType.values())
                .map(this::engineStatus)
                .toList());
    }

    private LambdaQueryWrapper<MangoJobInstanceEntity> instanceWrapper(MangoJobInstancePageQuery query) {
        String tenantId = MangoJobSupport.currentTenantId();
        return new LambdaQueryWrapper<MangoJobInstanceEntity>()
                .eq(MangoJobInstanceEntity::getTenantId, tenantId)
                .eq(query.getJobId() != null, MangoJobInstanceEntity::getJobId, query.getJobId())
                .eq(StringUtils.hasText(query.getStatus()), MangoJobInstanceEntity::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getTriggerType()), MangoJobInstanceEntity::getTriggerType, query.getTriggerType())
                .eq(StringUtils.hasText(query.getTriggerBatchNo()), MangoJobInstanceEntity::getTriggerBatchNo, query.getTriggerBatchNo())
                .ge(query.getTriggerTimeStart() != null, MangoJobInstanceEntity::getTriggerTime, query.getTriggerTimeStart())
                .le(query.getTriggerTimeEnd() != null, MangoJobInstanceEntity::getTriggerTime, query.getTriggerTimeEnd())
                .orderByDesc(MangoJobInstanceEntity::getTriggerTime);
    }

    private LambdaQueryWrapper<MangoJobLogIndexEntity> logWrapper(MangoJobLogPageQuery query) {
        String tenantId = MangoJobSupport.currentTenantId();
        return new LambdaQueryWrapper<MangoJobLogIndexEntity>()
                .eq(MangoJobLogIndexEntity::getTenantId, tenantId)
                .eq(query.getJobId() != null, MangoJobLogIndexEntity::getJobId, query.getJobId())
                .eq(query.getInstanceId() != null, MangoJobLogIndexEntity::getInstanceId, query.getInstanceId())
                .eq(StringUtils.hasText(query.getEngineType()), MangoJobLogIndexEntity::getEngineType, query.getEngineType())
                .orderByDesc(MangoJobLogIndexEntity::getCreatedAt);
    }

    private LambdaQueryWrapper<MangoJobWorkerSnapshotEntity> workerWrapper(MangoJobWorkerPageQuery query) {
        String tenantId = MangoJobSupport.currentTenantId();
        String keyword = MangoJobSupport.trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<MangoJobWorkerSnapshotEntity>()
                .eq(MangoJobWorkerSnapshotEntity::getTenantId, tenantId)
                .eq(StringUtils.hasText(query.getAppCode()), MangoJobWorkerSnapshotEntity::getAppCode, query.getAppCode())
                .eq(StringUtils.hasText(query.getStatus()), MangoJobWorkerSnapshotEntity::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getEngineType()), MangoJobWorkerSnapshotEntity::getEngineType, query.getEngineType())
                .like(StringUtils.hasText(keyword), MangoJobWorkerSnapshotEntity::getWorkerAddress, keyword)
                .orderByDesc(MangoJobWorkerSnapshotEntity::getLastHeartbeatAt);
    }

    private MangoJobEngineStatusVO engineStatus(JobEngineType engineType) {
        String tenantId = MangoJobSupport.currentTenantId();
        MangoJobEngineStatusVO vo = MangoJobSupport.emptyEngineStatus(engineType);
        vo.setPendingCount(countDefinitions(tenantId, engineType, JobSyncStatus.PENDING));
        vo.setFailedCount(countDefinitions(tenantId, engineType, JobSyncStatus.FAILED));
        vo.setSyncedCount(countDefinitions(tenantId, engineType, JobSyncStatus.SYNCED));
        List<MangoJobDefinitionEntity> records = definitionMapper.selectList(new LambdaQueryWrapper<MangoJobDefinitionEntity>()
                .eq(MangoJobDefinitionEntity::getTenantId, tenantId)
                .eq(MangoJobDefinitionEntity::getEngineType, engineType.name())
                .orderByDesc(MangoJobDefinitionEntity::getUpdatedAt)
                .last("limit 1"));
        if (!records.isEmpty()) {
            vo.setLastUpdatedAt(records.get(0).getUpdatedAt());
        }
        return vo;
    }

    private Long countDefinitions(String tenantId, JobEngineType engineType, JobSyncStatus syncStatus) {
        return definitionMapper.selectCount(new LambdaQueryWrapper<MangoJobDefinitionEntity>()
                .eq(MangoJobDefinitionEntity::getTenantId, tenantId)
                .eq(MangoJobDefinitionEntity::getEngineType, engineType.name())
                .eq(MangoJobDefinitionEntity::getSyncStatus, syncStatus.name()));
    }

    private void refreshRunningInstances(List<MangoJobInstanceEntity> records) {
        records.stream()
                .filter(this::needsRefresh)
                .forEach(instance -> {
                    MangoJobDefinitionEntity definition = definitionMapper.selectById(instance.getJobId());
                    if (definition != null && MangoJobSupport.currentTenantId().equals(definition.getTenantId())) {
                        engineSyncService.refreshInstance(definition, instance);
                    }
                });
    }

    private boolean needsRefresh(MangoJobInstanceEntity instance) {
        return StringUtils.hasText(instance.getEngineInstanceId())
                && (JobInstanceStatus.WAITING.name().equals(instance.getStatus())
                || JobInstanceStatus.RUNNING.name().equals(instance.getStatus()));
    }
}
