package io.mango.job.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.job.api.command.SyncMangoJobInstanceCommand;
import io.mango.job.api.enums.JobEngineType;
import io.mango.job.api.enums.JobInstanceStatus;
import io.mango.job.api.enums.JobScheduleType;
import io.mango.job.api.enums.JobSyncStatus;
import io.mango.job.api.enums.JobWorkerStatus;
import io.mango.job.api.query.MangoJobInstancePageQuery;
import io.mango.job.api.query.MangoJobLogPageQuery;
import io.mango.job.api.query.MangoJobWorkerPageQuery;
import io.mango.job.api.vo.MangoJobEngineStatusVO;
import io.mango.job.api.vo.MangoJobHandlerVO;
import io.mango.job.api.vo.MangoJobInstanceVO;
import io.mango.job.api.vo.MangoJobLogDetailVO;
import io.mango.job.api.vo.MangoJobLogIndexVO;
import io.mango.job.api.vo.MangoJobWorkerSnapshotVO;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import io.mango.job.core.entity.MangoJobLogIndexEntity;
import io.mango.job.core.entity.MangoJobWorkerSnapshotEntity;
import io.mango.job.core.entity.MangoJobAttemptEntity;
import io.mango.job.core.mapper.MangoJobAttemptMapper;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.job.core.mapper.MangoJobInstanceMapper;
import io.mango.job.core.mapper.MangoJobLogIndexMapper;
import io.mango.job.core.mapper.MangoJobWorkerSnapshotMapper;
import io.mango.job.support.service.IMangoJobHandlerRegistry;
import io.mango.job.core.service.IMangoJobQueryService;
import io.mango.job.core.service.engine.IMangoJobEngineRegistry;
import io.mango.job.core.service.engine.IMangoJobEngineSyncService;
import io.mango.job.core.service.engine.MangoJobLogRequest;
import io.mango.job.core.service.engine.MangoJobLogResult;
import io.mango.job.core.service.nativeengine.IMangoNativeJobRuntime;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Job 运行态查询服务实现。
 */
@Service
public class MangoJobQueryService implements IMangoJobQueryService {

    private static final int WORKER_ONLINE_HEARTBEAT_SECONDS = 600;

    private final MangoJobInstanceMapper instanceMapper;

    private final MangoJobAttemptMapper attemptMapper;

    private final MangoJobLogIndexMapper logIndexMapper;

    private final MangoJobWorkerSnapshotMapper workerSnapshotMapper;

    private final MangoJobDefinitionMapper definitionMapper;

    private final IMangoJobHandlerRegistry handlerRegistry;

    private final MangoJobDataSourceRouter dataSourceRouter;

    private final IMangoJobEngineSyncService engineSyncService;

    private final IMangoJobEngineRegistry engineRegistry;

    private final IMangoNativeJobRuntime nativeJobRuntime;

    public MangoJobQueryService(MangoJobInstanceMapper instanceMapper,
                                MangoJobAttemptMapper attemptMapper,
                                MangoJobLogIndexMapper logIndexMapper,
                                MangoJobWorkerSnapshotMapper workerSnapshotMapper,
                                MangoJobDefinitionMapper definitionMapper,
                                IMangoJobHandlerRegistry handlerRegistry,
                                MangoJobDataSourceRouter dataSourceRouter,
                                IMangoJobEngineSyncService engineSyncService,
                                IMangoJobEngineRegistry engineRegistry,
                                IMangoNativeJobRuntime nativeJobRuntime) {
        this.instanceMapper = instanceMapper;
        this.attemptMapper = attemptMapper;
        this.logIndexMapper = logIndexMapper;
        this.workerSnapshotMapper = workerSnapshotMapper;
        this.definitionMapper = definitionMapper;
        this.handlerRegistry = handlerRegistry;
        this.dataSourceRouter = dataSourceRouter;
        this.engineSyncService = engineSyncService;
        this.engineRegistry = engineRegistry;
        this.nativeJobRuntime = nativeJobRuntime;
    }

    @Override
    public PageResult<MangoJobInstanceVO> pageInstances(MangoJobInstancePageQuery query) {
        return dataSourceRouter.route(() -> {
            MangoJobInstancePageQuery resolved = query == null ? new MangoJobInstancePageQuery() : query;
            importScheduledInstances(resolved);
            refreshRunningInstances(resolved);
            IPage<MangoJobInstanceEntity> page = instanceMapper.selectPage(
                    new Page<>(resolved.getPage(), resolved.getSize()),
                    instanceWrapper(resolved));
            Map<Long, MangoJobDefinitionEntity> definitions = selectTenantDefinitions(page.getRecords());
            return PageResult.of(page.getRecords().stream()
                            .map(instance -> {
                                MangoJobInstanceVO vo = MangoJobSupport.toInstanceVO(instance,
                                        definitions.get(instance.getJobId()));
                                vo.setWorkerAddress(latestWorkerAddress(instance.getId()));
                                return vo;
                            })
                            .toList(),
                    page.getTotal(), page.getCurrent(), page.getSize());
        });
    }

    @Override
    public Boolean syncInstances(SyncMangoJobInstanceCommand command) {
        return dataSourceRouter.route(() -> {
            MangoJobInstancePageQuery resolved = toInstanceQuery(command);
            importScheduledInstances(resolved);
            refreshRunningInstances(resolved);
            return Boolean.TRUE;
        });
    }

    private MangoJobInstancePageQuery toInstanceQuery(SyncMangoJobInstanceCommand command) {
        MangoJobInstancePageQuery query = new MangoJobInstancePageQuery();
        if (command == null) {
            return query;
        }
        query.setJobId(command.getJobId());
        query.setTriggerTimeStart(command.getTriggerTimeStart());
        query.setTriggerTimeEnd(command.getTriggerTimeEnd());
        if (command.getSize() != null) {
            query.setSize(command.getSize());
        }
        return query;
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
    public MangoJobLogDetailVO detailLog(Long id) {
        return dataSourceRouter.route(() -> {
            Require.notNull(id, "日志 ID 不能为空");
            MangoJobLogIndexEntity logIndex = logIndexMapper.selectById(id);
            Require.notNull(logIndex, 404, "日志索引不存在");
            String tenantId = MangoJobSupport.currentTenantId();
            Require.isTrue(tenantId.equals(logIndex.getTenantId()), 404, "日志索引不存在");
            MangoJobInstanceEntity instance = selectTenantInstance(logIndex.getInstanceId(), tenantId);
            MangoJobDefinitionEntity definition = selectTenantDefinition(logIndex.getJobId(), tenantId);
            MangoJobLogDetailVO detail = toLogDetailVO(logIndex, instance, definition);
            fetchEngineLog(logIndex, instance, definition, detail);
            return detail;
        });
    }

    @Override
    public MangoJobLogDetailVO detailInstanceLog(Long instanceId) {
        return nativeJobRuntime.detailInstanceLog(instanceId);
    }

    @Override
    public PageResult<MangoJobWorkerSnapshotVO> pageWorkers(MangoJobWorkerPageQuery query) {
        return dataSourceRouter.route(() -> {
            MangoJobWorkerPageQuery resolved = query == null ? new MangoJobWorkerPageQuery() : query;
            expireStaleWorkers();
            IPage<MangoJobWorkerSnapshotEntity> page = workerSnapshotMapper.selectPage(
                    new Page<>(resolved.getPage(), resolved.getSize()),
                    workerWrapper(resolved));
            return PageResult.of(page.getRecords().stream()
                            .map(this::resolveWorkerStatus)
                            .map(MangoJobSupport::toWorkerVO)
                            .toList(),
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
                .eq(StringUtils.hasText(query.getServiceCode()), MangoJobWorkerSnapshotEntity::getServiceCode, query.getServiceCode())
                .eq(StringUtils.hasText(query.getWorkerGroup()), MangoJobWorkerSnapshotEntity::getWorkerGroup, query.getWorkerGroup())
                .eq(StringUtils.hasText(query.getTransportType()), MangoJobWorkerSnapshotEntity::getTransportType, query.getTransportType())
                .eq(StringUtils.hasText(query.getRegisterSource()), MangoJobWorkerSnapshotEntity::getRegisterSource, query.getRegisterSource())
                .eq(StringUtils.hasText(query.getStatus()), MangoJobWorkerSnapshotEntity::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getEngineType()), MangoJobWorkerSnapshotEntity::getEngineType, query.getEngineType())
                .notIn(MangoJobWorkerSnapshotEntity::getWorkerAddress, "N/A", "n/a", "UNKNOWN", "unknown", "NULL", "null", "-")
                .like(StringUtils.hasText(keyword), MangoJobWorkerSnapshotEntity::getWorkerAddress, keyword)
                .orderByDesc(MangoJobWorkerSnapshotEntity::getLastHeartbeatAt);
    }

    private void expireStaleWorkers() {
        String tenantId = MangoJobSupport.currentTenantId();
        workerSnapshotMapper.update(null, new LambdaUpdateWrapper<MangoJobWorkerSnapshotEntity>()
                .eq(MangoJobWorkerSnapshotEntity::getTenantId, tenantId)
                .eq(MangoJobWorkerSnapshotEntity::getStatus, JobWorkerStatus.ONLINE.name())
                .isNull(MangoJobWorkerSnapshotEntity::getLastHeartbeatAt)
                .set(MangoJobWorkerSnapshotEntity::getStatus, JobWorkerStatus.EXPIRED.name()));
        workerSnapshotMapper.update(null, new LambdaUpdateWrapper<MangoJobWorkerSnapshotEntity>()
                .eq(MangoJobWorkerSnapshotEntity::getTenantId, tenantId)
                .eq(MangoJobWorkerSnapshotEntity::getStatus, JobWorkerStatus.ONLINE.name())
                .lt(MangoJobWorkerSnapshotEntity::getLastHeartbeatAt,
                        LocalDateTime.now().minusSeconds(WORKER_ONLINE_HEARTBEAT_SECONDS))
                .set(MangoJobWorkerSnapshotEntity::getStatus, JobWorkerStatus.EXPIRED.name()));
    }

    private MangoJobWorkerSnapshotEntity resolveWorkerStatus(MangoJobWorkerSnapshotEntity worker) {
        if (!MangoJobSupport.hasValidWorkerAddress(worker.getWorkerAddress())) {
            worker.setStatus(JobWorkerStatus.UNKNOWN.name());
            return worker;
        }
        LocalDateTime heartbeat = worker.getLastHeartbeatAt();
        if (heartbeat == null || heartbeat.isBefore(LocalDateTime.now().minusSeconds(WORKER_ONLINE_HEARTBEAT_SECONDS))) {
            worker.setStatus(JobWorkerStatus.EXPIRED.name());
        }
        return worker;
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

    private void importScheduledInstances(MangoJobInstancePageQuery query) {
        selectImportDefinitions(query).forEach(definition -> engineSyncService.importScheduledInstances(
                definition, query.getTriggerTimeStart(), query.getTriggerTimeEnd(), importLimit(query)));
    }

    private List<MangoJobDefinitionEntity> selectImportDefinitions(MangoJobInstancePageQuery query) {
        String tenantId = MangoJobSupport.currentTenantId();
        LambdaQueryWrapper<MangoJobDefinitionEntity> wrapper = new LambdaQueryWrapper<MangoJobDefinitionEntity>()
                .eq(MangoJobDefinitionEntity::getTenantId, tenantId)
                .eq(query.getJobId() != null, MangoJobDefinitionEntity::getId, query.getJobId())
                .ne(MangoJobDefinitionEntity::getScheduleType, JobScheduleType.MANUAL.name())
                .isNotNull(MangoJobDefinitionEntity::getEngineJobId);
        if (query.getJobId() == null) {
            wrapper.orderByDesc(MangoJobDefinitionEntity::getUpdatedAt).last("limit 20");
        }
        return definitionMapper.selectList(wrapper);
    }

    private int importLimit(MangoJobInstancePageQuery query) {
        long size = query.getSize();
        if (size <= 0) {
            return 20;
        }
        return (int) Math.min(Math.max(size, 20), 100);
    }

    private void refreshRunningInstances(MangoJobInstancePageQuery query) {
        List<MangoJobInstanceEntity> records = instanceMapper.selectList(instanceWrapper(query).last("limit " + importLimit(query)));
        records.stream()
                .filter(this::needsRefresh)
                .forEach(instance -> {
                    MangoJobDefinitionEntity definition = definitionMapper.selectById(instance.getJobId());
                    if (definition != null && MangoJobSupport.currentTenantId().equals(definition.getTenantId())) {
                        engineSyncService.refreshInstance(definition, instance);
                    }
                });
    }

    private Map<Long, MangoJobDefinitionEntity> selectTenantDefinitions(List<MangoJobInstanceEntity> records) {
        List<Long> jobIds = records.stream()
                .map(MangoJobInstanceEntity::getJobId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (jobIds.isEmpty()) {
            return Map.of();
        }
        String tenantId = MangoJobSupport.currentTenantId();
        return definitionMapper.selectList(new LambdaQueryWrapper<MangoJobDefinitionEntity>()
                        .eq(MangoJobDefinitionEntity::getTenantId, tenantId)
                        .in(MangoJobDefinitionEntity::getId, jobIds))
                .stream()
                .collect(Collectors.toMap(MangoJobDefinitionEntity::getId, Function.identity()));
    }

    private String latestWorkerAddress(Long instanceId) {
        if (instanceId == null) {
            return null;
        }
        List<MangoJobAttemptEntity> attempts = attemptMapper.selectList(
                new LambdaQueryWrapper<MangoJobAttemptEntity>()
                        .eq(MangoJobAttemptEntity::getInstanceId, instanceId)
                        .orderByDesc(MangoJobAttemptEntity::getAttemptNo)
                        .last("limit 1"));
        return attempts.isEmpty() ? null : attempts.get(0).getWorkerAddressSnapshot();
    }

    private boolean needsRefresh(MangoJobInstanceEntity instance) {
        return StringUtils.hasText(instance.getEngineInstanceId())
                && (JobInstanceStatus.WAITING.name().equals(instance.getStatus())
                || JobInstanceStatus.RUNNING.name().equals(instance.getStatus()));
    }

    private MangoJobInstanceEntity selectTenantInstance(Long instanceId, String tenantId) {
        if (instanceId == null) {
            return null;
        }
        MangoJobInstanceEntity instance = instanceMapper.selectById(instanceId);
        if (instance == null || !tenantId.equals(instance.getTenantId())) {
            return null;
        }
        return instance;
    }

    private MangoJobDefinitionEntity selectTenantDefinition(Long jobId, String tenantId) {
        if (jobId == null) {
            return null;
        }
        MangoJobDefinitionEntity definition = definitionMapper.selectById(jobId);
        if (definition == null || !tenantId.equals(definition.getTenantId())) {
            return null;
        }
        return definition;
    }

    private MangoJobLogDetailVO toLogDetailVO(MangoJobLogIndexEntity logIndex,
                                              MangoJobInstanceEntity instance,
                                              MangoJobDefinitionEntity definition) {
        MangoJobLogDetailVO detail = new MangoJobLogDetailVO();
        detail.setId(logIndex.getId());
        detail.setTenantId(logIndex.getTenantId());
        detail.setJobId(logIndex.getJobId());
        detail.setInstanceId(logIndex.getInstanceId());
        detail.setEngineType(logIndex.getEngineType());
        detail.setEngineInstanceId(logIndex.getEngineInstanceId());
        detail.setLogLocation(logIndex.getLogLocation());
        detail.setReadOffset(logIndex.getReadOffset());
        detail.setErrorSummary(logIndex.getErrorSummary());
        detail.setLastFetchedAt(logIndex.getLastFetchedAt());
        detail.setCreatedAt(logIndex.getCreatedAt());
        detail.setLogSource("MANGO_LOG_INDEX");
        if (definition != null) {
            detail.setJobCode(definition.getJobCode());
            detail.setJobName(definition.getJobName());
        }
        if (instance != null) {
            detail.setInstanceStatus(instance.getStatus());
            detail.setTriggerBatchNo(instance.getTriggerBatchNo());
        }
        return detail;
    }

    private void fetchEngineLog(MangoJobLogIndexEntity logIndex,
                                MangoJobInstanceEntity instance,
                                MangoJobDefinitionEntity definition,
                                MangoJobLogDetailVO detail) {
        if (JobEngineType.MANGO_NATIVE.name().equals(logIndex.getEngineType()) && instance != null) {
            MangoJobLogDetailVO nativeDetail = nativeJobRuntime.detailInstanceLog(instance.getId());
            detail.setLogSource(nativeDetail.getLogSource());
            detail.setNativeLogAvailable(nativeDetail.getNativeLogAvailable());
            detail.setLogFetchStatus(nativeDetail.getLogFetchStatus());
            detail.setNativeLogContent(nativeDetail.getNativeLogContent());
            detail.setContent(nativeDetail.getContent());
            detail.setEngineResult(nativeDetail.getEngineResult());
            detail.setReadOffset(nativeDetail.getReadOffset());
            detail.setLastFetchedAt(nativeDetail.getLastFetchedAt());
            return;
        }
        if (!StringUtils.hasText(logIndex.getEngineType())) {
            return;
        }
        engineRegistry.findEngine(logIndex.getEngineType()).ifPresentOrElse(engine -> {
            MangoJobLogRequest request = new MangoJobLogRequest();
            request.setLogIndex(logIndex);
            request.setInstance(instance);
            request.setDefinition(definition);
            MangoJobLogResult result = engine.fetchLog(request);
            detail.setLogSource(StringUtils.hasText(result.getSource()) ? result.getSource() : logIndex.getEngineType());
            detail.setNativeLogAvailable(result.isNativeLogAvailable());
            detail.setLogFetchStatus(result.getLogFetchStatus());
            detail.setNativeLogContent(result.getNativeLogContent());
            detail.setContent(result.getContent());
            detail.setEngineResult(result.getEngineResult());
            if (!result.isSuccess() && StringUtils.hasText(result.getErrorSummary())) {
                detail.setErrorSummary(result.getErrorSummary());
            }
        }, () -> detail.setErrorSummary("调度引擎未注册：" + logIndex.getEngineType()));
    }
}
