package io.mango.job.core.service.nativeengine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.job.api.enums.JobAttemptStatus;
import io.mango.job.api.enums.JobDefinitionStatus;
import io.mango.job.api.enums.JobEngineType;
import io.mango.job.api.enums.JobHandleStatus;
import io.mango.job.api.enums.JobInstanceStatus;
import io.mango.job.api.enums.JobScheduleType;
import io.mango.job.api.enums.JobSyncStatus;
import io.mango.job.api.enums.JobTransportType;
import io.mango.job.api.enums.JobTriggerType;
import io.mango.job.api.enums.JobWorkerRegisterSource;
import io.mango.job.api.enums.JobWorkerStatus;
import io.mango.job.api.command.MangoJobWorkerExecuteCommand;
import io.mango.job.api.command.RegisterMangoJobWorkerCommand;
import io.mango.job.api.vo.MangoJobLogDetailVO;
import io.mango.job.api.vo.MangoJobHandlerVO;
import io.mango.job.api.vo.MangoJobWorkerExecuteResultVO;
import io.mango.job.api.vo.MangoJobWorkerExecutionLogVO;
import io.mango.job.core.entity.MangoJobAttemptEntity;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import io.mango.job.core.entity.MangoJobLogChunkEntity;
import io.mango.job.core.entity.MangoJobLogIndexEntity;
import io.mango.job.core.entity.MangoJobScheduleCursorEntity;
import io.mango.job.core.entity.MangoJobWorkerCapabilityEntity;
import io.mango.job.core.entity.MangoJobWorkerSnapshotEntity;
import io.mango.job.core.mapper.MangoJobAttemptMapper;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.job.core.mapper.MangoJobInstanceMapper;
import io.mango.job.core.mapper.MangoJobLogChunkMapper;
import io.mango.job.core.mapper.MangoJobLogIndexMapper;
import io.mango.job.core.mapper.MangoJobScheduleCursorMapper;
import io.mango.job.core.mapper.MangoJobWorkerCapabilityMapper;
import io.mango.job.core.mapper.MangoJobWorkerSnapshotMapper;
import io.mango.job.core.service.impl.MangoJobDataSourceRouter;
import io.mango.job.core.service.IMangoJobWorkerRegistryService;
import io.mango.job.support.nativeengine.MangoJobTransportAddresses;
import io.mango.job.support.nativeengine.MangoJobWorkerDispatchRequest;
import io.mango.job.support.nativeengine.MangoJobWorkerTransportRegistry;
import io.mango.job.support.nativeengine.MangoNativeJobProperties;
import io.mango.job.support.service.IMangoJobHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mango 原生 Job 内嵌运行时。
 */
@Service
public class MangoNativeJobRuntime implements IMangoNativeJobRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger(MangoNativeJobRuntime.class);

    private static final int LOG_DETAIL_LIMIT = 1000;

    private final MangoJobDefinitionMapper definitionMapper;

    private final MangoJobInstanceMapper instanceMapper;

    private final MangoJobScheduleCursorMapper cursorMapper;

    private final MangoJobAttemptMapper attemptMapper;

    private final MangoJobLogChunkMapper logChunkMapper;

    private final MangoJobLogIndexMapper logIndexMapper;

    private final MangoJobWorkerSnapshotMapper workerSnapshotMapper;

    private final MangoJobWorkerCapabilityMapper workerCapabilityMapper;

    private final MangoJobDataSourceRouter dataSourceRouter;

    private final MangoJobScheduleCalculator scheduleCalculator;

    private final MangoJobIdempotencyKeyService idempotencyKeyService;

    private final MangoJobLeaseService leaseService;

    private final MangoNativeJobProperties properties;

    private final MangoJobWorkerTransportRegistry transportRegistry;

    private final IMangoJobHandlerRegistry handlerRegistry;

    private final IMangoJobWorkerRegistryService workerRegistryService;

    private final MangoJobAlarmNotificationService alarmNotificationService;

    private final String workerAddress;

    private final String workerInstanceId;

    public MangoNativeJobRuntime(MangoJobDefinitionMapper definitionMapper,
                                 MangoJobInstanceMapper instanceMapper,
                                 MangoJobScheduleCursorMapper cursorMapper,
                                 MangoJobAttemptMapper attemptMapper,
                                 MangoJobLogChunkMapper logChunkMapper,
                                 MangoJobLogIndexMapper logIndexMapper,
                                 MangoJobWorkerSnapshotMapper workerSnapshotMapper,
                                 MangoJobWorkerCapabilityMapper workerCapabilityMapper,
                                 MangoJobDataSourceRouter dataSourceRouter,
                                 MangoJobScheduleCalculator scheduleCalculator,
                                 MangoJobIdempotencyKeyService idempotencyKeyService,
                                 MangoJobLeaseService leaseService,
                                 MangoNativeJobProperties properties,
                                 Environment environment,
                                 MangoJobWorkerTransportRegistry transportRegistry,
                                 IMangoJobHandlerRegistry handlerRegistry,
                                 IMangoJobWorkerRegistryService workerRegistryService,
                                 MangoJobAlarmNotificationService alarmNotificationService) {
        this.definitionMapper = definitionMapper;
        this.instanceMapper = instanceMapper;
        this.cursorMapper = cursorMapper;
        this.attemptMapper = attemptMapper;
        this.logChunkMapper = logChunkMapper;
        this.logIndexMapper = logIndexMapper;
        this.workerSnapshotMapper = workerSnapshotMapper;
        this.workerCapabilityMapper = workerCapabilityMapper;
        this.dataSourceRouter = dataSourceRouter;
        this.scheduleCalculator = scheduleCalculator;
        this.idempotencyKeyService = idempotencyKeyService;
        this.leaseService = leaseService;
        this.properties = properties;
        this.workerAddress = resolveWorkerAddress(environment);
        this.workerInstanceId = resolveWorkerInstanceId(environment, workerAddress);
        this.transportRegistry = transportRegistry;
        this.handlerRegistry = handlerRegistry;
        this.workerRegistryService = workerRegistryService;
        this.alarmNotificationService = alarmNotificationService;
    }

    @Override
    public void syncDefinition(MangoJobDefinitionEntity definition) {
        dataSourceRouter.route(() -> {
            definition.setEngineType(JobEngineType.MANGO_NATIVE.name());
            definition.setEngineAppId(definition.getAppCode());
            definition.setEngineJobId(String.valueOf(definition.getId()));
            definition.setSyncStatus(JobSyncStatus.SYNCED.name());
            definition.setSyncError(null);
            definitionMapper.updateById(definition);
            upsertScheduleCursor(definition);
            if (properties.isEmbeddedWorkerEnabled()) {
                upsertEmbeddedWorkers(definition.getTenantId());
            }
            return Boolean.TRUE;
        });
    }

    @Override
    public void deleteDefinition(MangoJobDefinitionEntity definition) {
        dataSourceRouter.route(() -> {
            cursorMapper.delete(new LambdaQueryWrapper<MangoJobScheduleCursorEntity>()
                    .eq(MangoJobScheduleCursorEntity::getJobId, definition.getId()));
            return Boolean.TRUE;
        });
    }

    @Override
    public void trigger(MangoJobDefinitionEntity definition,
                        MangoJobInstanceEntity instance,
                        String batchNo,
                        String paramValue) {
        dataSourceRouter.route(() -> {
            fillInstanceSnapshot(definition, instance, JobTriggerType.MANUAL, LocalDateTime.now(),
                    idempotencyKeyService.manual(definition.getId(), batchNo));
            instanceMapper.updateById(instance);
            executeInstance(definition, instance, resolveParam(definition, paramValue));
            return Boolean.TRUE;
        });
    }

    @Override
    public void tick() {
        dataSourceRouter.route(() -> {
            LocalDateTime now = LocalDateTime.now();
            List<MangoJobScheduleCursorEntity> dueCursors = cursorMapper.selectList(
                    new LambdaQueryWrapper<MangoJobScheduleCursorEntity>()
                            .le(MangoJobScheduleCursorEntity::getNextFireTime, now)
                            .and(wrapper -> wrapper
                                    .isNull(MangoJobScheduleCursorEntity::getLockUntil)
                                    .or()
                                    .lt(MangoJobScheduleCursorEntity::getLockUntil, now))
                            .orderByAsc(MangoJobScheduleCursorEntity::getNextFireTime)
                            .last("limit " + Math.max(1, properties.getScanLimit())));
            dueCursors.forEach(cursor -> {
                try {
                    triggerScheduledCursor(cursor, now);
                } catch (RuntimeException ex) {
                    LOGGER.warn("Mango native job tick failed, cursorId={}, jobId={}",
                            cursor.getId(), cursor.getJobId(), ex);
                }
            });
            return Boolean.TRUE;
        });
    }

    @Override
    public void registerEmbeddedWorkers(String tenantId) {
        Require.notBlank(tenantId, "内嵌 Worker 注册租户不能为空");
        dataSourceRouter.route(() -> {
            upsertEmbeddedWorkers(tenantId.trim());
            return Boolean.TRUE;
        });
    }

    @Override
    public void importScheduledInstances(MangoJobDefinitionEntity definition,
                                         LocalDateTime triggerTimeStart,
                                         LocalDateTime triggerTimeEnd,
                                         int limit) {
        // Native scheduled instances are produced by the JobCenter scheduler.
        // Import is only meaningful for external engines whose instances live outside Mango.
    }

    @Override
    public MangoJobLogDetailVO detailInstanceLog(Long instanceId) {
        return dataSourceRouter.route(() -> {
            Require.notNull(instanceId, "实例 ID 不能为空");
            String tenantId = currentTenantId();
            MangoJobInstanceEntity instance = instanceMapper.selectById(instanceId);
            Require.notNull(instance, 404, "执行实例不存在");
            Require.isTrue(tenantId.equals(instance.getTenantId()), 404, "执行实例不存在");
            MangoJobDefinitionEntity definition = definitionMapper.selectById(instance.getJobId());
            MangoJobLogDetailVO detail = new MangoJobLogDetailVO();
            detail.setTenantId(tenantId);
            detail.setJobId(instance.getJobId());
            detail.setJobCode(instance.getJobCode());
            detail.setJobName(definition == null ? instance.getJobNameSnapshot() : definition.getJobName());
            detail.setInstanceId(instance.getId());
            detail.setInstanceStatus(instance.getStatus());
            detail.setTriggerBatchNo(instance.getTriggerBatchNo());
            detail.setEngineType(JobEngineType.MANGO_NATIVE.name());
            detail.setEngineInstanceId(instance.getEngineInstanceId());
            detail.setLogLocation("mango-job://jobs/" + instance.getJobId() + "/instances/" + instance.getId());
            detail.setLogSource("MANGO_NATIVE");
            detail.setNativeLogAvailable(Boolean.TRUE);
            detail.setLogFetchStatus("SUCCESS");
            detail.setErrorSummary(instance.getErrorSummary());
            detail.setCreatedAt(instance.getCreatedAt());
            List<MangoJobLogChunkEntity> chunks = logChunkMapper.selectList(
                    new LambdaQueryWrapper<MangoJobLogChunkEntity>()
                            .eq(MangoJobLogChunkEntity::getTenantId, tenantId)
                            .eq(MangoJobLogChunkEntity::getInstanceId, instanceId)
                            .orderByAsc(MangoJobLogChunkEntity::getSequenceNo)
                            .last("limit " + LOG_DETAIL_LIMIT));
            String content = chunks.stream()
                    .map(this::formatLogChunk)
                    .collect(Collectors.joining(System.lineSeparator()));
            detail.setNativeLogContent(content);
            detail.setContent(content);
            detail.setEngineResult(instance.getResultSummary());
            if (!chunks.isEmpty()) {
                detail.setReadOffset(chunks.get(chunks.size() - 1).getSequenceNo());
                detail.setLastFetchedAt(chunks.get(chunks.size() - 1).getLogTime());
            }
            return detail;
        });
    }

    private void triggerScheduledCursor(MangoJobScheduleCursorEntity cursor, LocalDateTime now) {
        MangoJobDefinitionEntity definition = definitionMapper.selectById(cursor.getJobId());
        if (!shouldSchedule(definition)) {
            return;
        }
        LocalDateTime scheduledFireTime = cursor.getNextFireTime();
        String lockOwner = workerAddress;
        LocalDateTime lockUntil = now.plusSeconds(30);
        int locked = cursorMapper.update(null, new LambdaUpdateWrapper<MangoJobScheduleCursorEntity>()
                .eq(MangoJobScheduleCursorEntity::getId, cursor.getId())
                .eq(MangoJobScheduleCursorEntity::getNextFireTime, scheduledFireTime)
                .and(wrapper -> wrapper
                        .isNull(MangoJobScheduleCursorEntity::getLockUntil)
                        .or()
                        .lt(MangoJobScheduleCursorEntity::getLockUntil, now))
                .set(MangoJobScheduleCursorEntity::getLockOwner, lockOwner)
                .set(MangoJobScheduleCursorEntity::getLockUntil, lockUntil)
                .set(MangoJobScheduleCursorEntity::getLastScanAt, now));
        if (locked <= 0) {
            return;
        }
        try {
            Optional<MangoJobInstanceEntity> instance = createScheduledInstance(definition, scheduledFireTime, now);
            instance.ifPresent(value -> executeInstance(definition, value, definition.getParamValue()));
        } finally {
            LocalDateTime nextFireTime = scheduleCalculator.nextFireTime(definition, scheduledFireTime);
            cursorMapper.update(null, new LambdaUpdateWrapper<MangoJobScheduleCursorEntity>()
                    .eq(MangoJobScheduleCursorEntity::getId, cursor.getId())
                    .set(MangoJobScheduleCursorEntity::getLastFireTime, scheduledFireTime)
                    .set(MangoJobScheduleCursorEntity::getNextFireTime, nextFireTime)
                    .set(MangoJobScheduleCursorEntity::getLockOwner, null)
                    .set(MangoJobScheduleCursorEntity::getLockUntil, null));
        }
    }

    private Optional<MangoJobInstanceEntity> createScheduledInstance(MangoJobDefinitionEntity definition,
                                                                    LocalDateTime scheduledFireTime,
                                                                    LocalDateTime now) {
        String idempotencyKey = idempotencyKeyService.scheduled(definition.getId(),
                definition.getVersion() == null ? 0 : definition.getVersion(), scheduledFireTime);
        MangoJobInstanceEntity existing = instanceMapper.selectOne(new LambdaQueryWrapper<MangoJobInstanceEntity>()
                .eq(MangoJobInstanceEntity::getTenantId, definition.getTenantId())
                .eq(MangoJobInstanceEntity::getJobId, definition.getId())
                .eq(MangoJobInstanceEntity::getIdempotencyKey, idempotencyKey)
                .last("limit 1"));
        if (existing != null) {
            return Optional.empty();
        }
        MangoJobInstanceEntity instance = new MangoJobInstanceEntity();
        instance.setTenantId(definition.getTenantId());
        instance.setJobId(definition.getId());
        instance.setTriggerBatchNo("schedule-" + definition.getId() + "-" + scheduledFireTime);
        instance.setTraceId(UUID.randomUUID().toString());
        instance.setEngineType(JobEngineType.MANGO_NATIVE.name());
        fillInstanceSnapshot(definition, instance, JobTriggerType.SCHEDULED, now, idempotencyKey);
        instance.setScheduledFireTime(scheduledFireTime);
        instanceMapper.insert(instance);
        instance.setEngineInstanceId(String.valueOf(instance.getId()));
        instanceMapper.updateById(instance);
        return Optional.of(instance);
    }

    private void executeInstance(MangoJobDefinitionEntity definition,
                                 MangoJobInstanceEntity instance,
                                 String paramValue) {
        MangoJobAttemptEntity attempt = null;
        MangoJobWorkerSnapshotEntity worker;
        try {
            worker = selectWorker(definition);
            attempt = createAttempt(definition, instance, worker);
        } catch (RuntimeException ex) {
            failInstanceBeforeDispatch(definition, instance, ex.getMessage());
            throw ex;
        }
        appendLog(instance, attempt, "INFO", "mango-job-runtime",
                "Job attempt leased by " + worker.getWorkerAddress());
        LocalDateTime start = LocalDateTime.now();
        try {
            transitionToRunning(instance, attempt, start);
            JobTransportType transportType = resolveTransportType(worker);
            MangoJobWorkerExecuteResultVO result = transportRegistry.requireTransport(transportType)
                    .execute(new MangoJobWorkerDispatchRequest(worker.getWorkerAddress(),
                            toWorkerCommand(definition, instance, paramValue)));
            writeWorkerLogs(instance, attempt, result);
            finishAttempt(definition, instance, attempt, result);
        } catch (RuntimeException ex) {
            failAttempt(definition, instance, attempt, ex.getMessage());
            throw ex;
        }
    }

    private MangoJobAttemptEntity createAttempt(MangoJobDefinitionEntity definition,
                                                MangoJobInstanceEntity instance,
                                                MangoJobWorkerSnapshotEntity worker) {
        MangoJobAttemptEntity attempt = new MangoJobAttemptEntity();
        attempt.setTenantId(definition.getTenantId());
        attempt.setInstanceId(instance.getId());
        attempt.setJobId(definition.getId());
        attempt.setAttemptNo(nextAttemptNo(instance.getId()));
        attempt.setWorkerId(worker.getId());
        attempt.setWorkerAddressSnapshot(worker.getWorkerAddress());
        attempt.setServiceCodeSnapshot(worker.getServiceCode());
        attempt.setWorkerGroupSnapshot(worker.getWorkerGroup());
        attempt.setStatus(JobAttemptStatus.LEASED.name());
        attempt.setDispatchTime(LocalDateTime.now());
        attempt.setLastHeartbeatAt(LocalDateTime.now());
        leaseService.grant(attempt, worker.getWorkerAddress(), properties.getLeaseSeconds());
        attemptMapper.insert(attempt);
        instance.setAttemptCount(attempt.getAttemptNo());
        instance.setStatus(JobInstanceStatus.DISPATCHED.name());
        instance.setEngineInstanceId(String.valueOf(instance.getId()));
        instanceMapper.updateById(instance);
        return attempt;
    }

    private int nextAttemptNo(Long instanceId) {
        List<MangoJobAttemptEntity> attempts = attemptMapper.selectList(new LambdaQueryWrapper<MangoJobAttemptEntity>()
                .eq(MangoJobAttemptEntity::getInstanceId, instanceId)
                .orderByDesc(MangoJobAttemptEntity::getAttemptNo)
                .last("limit 1"));
        return attempts.isEmpty() ? 1 : attempts.get(0).getAttemptNo() + 1;
    }

    private void transitionToRunning(MangoJobInstanceEntity instance, MangoJobAttemptEntity attempt, LocalDateTime start) {
        attempt.setStatus(JobAttemptStatus.RUNNING.name());
        attempt.setStartTime(start);
        attempt.setLastHeartbeatAt(start);
        attemptMapper.updateById(attempt);
        instance.setStatus(JobInstanceStatus.RUNNING.name());
        instance.setStartTime(start);
        instance.setActualFireTime(start);
        instanceMapper.updateById(instance);
    }

    private void finishAttempt(MangoJobDefinitionEntity definition,
                               MangoJobInstanceEntity instance,
                               MangoJobAttemptEntity attempt,
                               MangoJobWorkerExecuteResultVO result) {
        LocalDateTime end = LocalDateTime.now();
        JobHandleStatus handleStatus = result == null ? JobHandleStatus.SUCCESS : result.getStatus();
        boolean success = handleStatus == null || handleStatus == JobHandleStatus.SUCCESS;
        attempt.setStatus(success ? JobAttemptStatus.SUCCEEDED.name() : JobAttemptStatus.FAILED.name());
        attempt.setEndTime(end);
        attempt.setExitCode(success ? "0" : "1");
        attempt.setErrorSummary(success ? null : result.getMessage());
        attempt.setResultPayload(result == null ? null : result.getResult());
        attemptMapper.updateById(attempt);

        instance.setStatus(success ? JobInstanceStatus.SUCCESS.name() : JobInstanceStatus.FAILED.name());
        instance.setEndTime(end);
        instance.setDurationMillis(Duration.between(instance.getStartTime(), end).toMillis());
        instance.setErrorSummary(success ? null : result.getMessage());
        instance.setResultSummary(result == null ? null : firstText(result.getMessage(), result.getResult()));
        instanceMapper.updateById(instance);
        appendLog(instance, attempt, success ? "INFO" : "ERROR", "mango-job-result",
                "handlerResult=" + instance.getResultSummary());
        upsertLogIndex(definition, instance);
        if (!success) {
            notifyInstanceFailed(definition, instance, instance.getErrorSummary());
        }
    }

    private void failAttempt(MangoJobDefinitionEntity definition,
                             MangoJobInstanceEntity instance,
                             MangoJobAttemptEntity attempt,
                             String message) {
        LocalDateTime end = LocalDateTime.now();
        attempt.setStatus(JobAttemptStatus.FAILED.name());
        attempt.setEndTime(end);
        attempt.setExitCode("1");
        attempt.setErrorSummary(message);
        attemptMapper.updateById(attempt);
        instance.setStatus(JobInstanceStatus.FAILED.name());
        instance.setEndTime(end);
        instance.setDurationMillis(instance.getStartTime() == null ? null : Duration.between(instance.getStartTime(), end).toMillis());
        instance.setErrorSummary(message);
        instanceMapper.updateById(instance);
        appendLog(instance, attempt, "ERROR", "mango-job-result", message);
        upsertLogIndex(definition, instance);
        notifyInstanceFailed(definition, instance, message);
    }

    private void failInstanceBeforeDispatch(MangoJobDefinitionEntity definition,
                                            MangoJobInstanceEntity instance,
                                            String message) {
        LocalDateTime end = LocalDateTime.now();
        instance.setStatus(JobInstanceStatus.FAILED.name());
        instance.setEndTime(end);
        instance.setDurationMillis(instance.getStartTime() == null ? null
                : Duration.between(instance.getStartTime(), end).toMillis());
        instance.setErrorSummary(message);
        instanceMapper.updateById(instance);
        appendLog(instance, null, "ERROR", "mango-job-dispatch", message);
        upsertLogIndex(definition, instance);
        notifyInstanceFailed(definition, instance, message);
    }

    private void notifyInstanceFailed(MangoJobDefinitionEntity definition,
                                      MangoJobInstanceEntity instance,
                                      String message) {
        String result = alarmNotificationService.notifyInstanceFailed(definition, instance, message);
        if (StringUtils.hasText(result)) {
            appendLog(instance, null, result.contains("失败") || result.contains("异常") ? "WARN" : "INFO",
                    "mango-job-alarm", result);
        }
    }

    private void writeWorkerLogs(MangoJobInstanceEntity instance,
                                 MangoJobAttemptEntity attempt,
                                 MangoJobWorkerExecuteResultVO result) {
        if (result == null || result.getLogs() == null) {
            return;
        }
        for (MangoJobWorkerExecutionLogVO log : result.getLogs()) {
            if (log != null && StringUtils.hasText(log.getContent())) {
                appendLog(instance, attempt, log.getLevel(), log.getLoggerName(), log.getContent());
            }
        }
    }

    private void appendLog(MangoJobInstanceEntity instance,
                           MangoJobAttemptEntity attempt,
                           String level,
                           String loggerName,
                           String content) {
        MangoJobLogChunkEntity chunk = new MangoJobLogChunkEntity();
        chunk.setTenantId(instance.getTenantId());
        chunk.setInstanceId(instance.getId());
        chunk.setAttemptId(attempt == null ? null : attempt.getId());
        chunk.setSequenceNo(nextLogSequence(instance.getId(), attempt == null ? null : attempt.getId()));
        chunk.setLogTime(LocalDateTime.now());
        chunk.setLevel(level);
        chunk.setLoggerName(loggerName);
        chunk.setThreadName(Thread.currentThread().getName());
        chunk.setContent(content);
        chunk.setRedacted(0);
        logChunkMapper.insert(chunk);
    }

    private long nextLogSequence(Long instanceId, Long attemptId) {
        LambdaQueryWrapper<MangoJobLogChunkEntity> wrapper = new LambdaQueryWrapper<MangoJobLogChunkEntity>()
                .eq(MangoJobLogChunkEntity::getInstanceId, instanceId)
                .orderByDesc(MangoJobLogChunkEntity::getSequenceNo)
                .last("limit 1");
        if (attemptId == null) {
            wrapper.isNull(MangoJobLogChunkEntity::getAttemptId);
        } else {
            wrapper.eq(MangoJobLogChunkEntity::getAttemptId, attemptId);
        }
        MangoJobLogChunkEntity last = logChunkMapper.selectOne(wrapper);
        return last == null ? 1L : last.getSequenceNo() + 1L;
    }

    private void upsertScheduleCursor(MangoJobDefinitionEntity definition) {
        if (JobScheduleType.MANUAL.name().equals(definition.getScheduleType())) {
            cursorMapper.delete(new LambdaQueryWrapper<MangoJobScheduleCursorEntity>()
                    .eq(MangoJobScheduleCursorEntity::getJobId, definition.getId()));
            return;
        }
        MangoJobScheduleCursorEntity cursor = cursorMapper.selectOne(new LambdaQueryWrapper<MangoJobScheduleCursorEntity>()
                .eq(MangoJobScheduleCursorEntity::getJobId, definition.getId())
                .last("limit 1"));
        if (cursor == null) {
            cursor = new MangoJobScheduleCursorEntity();
            cursor.setTenantId(definition.getTenantId());
            cursor.setJobId(definition.getId());
            cursor.setScheduleVersion(definition.getVersion() == null ? 0 : definition.getVersion());
            cursor.setMisfirePolicy(definition.getMisfireStrategy());
            cursor.setNextFireTime(scheduleCalculator.nextFireTime(definition, LocalDateTime.now()));
            cursorMapper.insert(cursor);
            return;
        }
        cursor.setScheduleVersion(definition.getVersion() == null ? 0 : definition.getVersion());
        cursor.setMisfirePolicy(definition.getMisfireStrategy());
        if (cursor.getNextFireTime() == null || JobDefinitionStatus.ENABLED.name().equals(definition.getStatus())) {
            cursor.setNextFireTime(scheduleCalculator.nextFireTime(definition, LocalDateTime.now()));
        }
        cursorMapper.updateById(cursor);
    }

    private void upsertEmbeddedWorkers(String tenantId) {
        if (!properties.isEmbeddedWorkerEnabled()) {
            LOGGER.debug("Mango embedded job worker upsert skipped, embeddedWorkerEnabled=false");
            return;
        }
        List<MangoJobHandlerVO> handlers = handlerRegistry.listHandlers();
        if (handlers.isEmpty()) {
            LOGGER.info("Mango embedded job worker upsert skipped, no job handler registered, tenantId={}, workerAddress={}",
                    tenantId, workerAddress);
            return;
        }
        LOGGER.info("Mango embedded job worker upsert started, tenantId={}, handlerCount={}, workerAddress={}",
                tenantId, handlers.size(), workerAddress);
        handlers.stream()
                .collect(Collectors.groupingBy(this::workerRegistrationKey))
                .values()
                .forEach(groupedHandlers -> registerEmbeddedWorker(tenantId, groupedHandlers));
    }

    private void registerEmbeddedWorker(String tenantId, List<MangoJobHandlerVO> handlers) {
        if (handlers.isEmpty()) {
            return;
        }
        MangoJobHandlerVO first = handlers.get(0);
        RegisterMangoJobWorkerCommand command = new RegisterMangoJobWorkerCommand();
        command.setTenantId(tenantId);
        command.setAppCode(first.getAppCode());
        command.setServiceCode(first.getServiceCode());
        command.setWorkerGroup(first.getWorkerGroup());
        command.setWorkerAddress(workerAddress);
        command.setRuntimeAddress(workerAddress);
        command.setTransportType(JobTransportType.IN_MEMORY);
        command.setRegisterSource(JobWorkerRegisterSource.EMBEDDED_AUTO);
        command.setWorkerInstanceId(workerInstanceId);
        command.getHandlers().addAll(handlers);
        Long workerId = workerRegistryService.registerWorker(command);
        LOGGER.info("Mango embedded job worker upserted, workerId={}, appCode={}, serviceCode={}, workerGroup={}, handlerCount={}, workerAddress={}",
                workerId, first.getAppCode(), first.getServiceCode(), first.getWorkerGroup(), handlers.size(), workerAddress);
    }

    private MangoJobWorkerSnapshotEntity selectWorker(MangoJobDefinitionEntity definition) {
        if (properties.isEmbeddedWorkerEnabled()) {
            upsertEmbeddedWorkers(definition.getTenantId());
        }
        Set<Long> workerIds = workerCapabilityMapper.selectList(
                        new LambdaQueryWrapper<MangoJobWorkerCapabilityEntity>()
                                .eq(MangoJobWorkerCapabilityEntity::getTenantId, definition.getTenantId())
                                .eq(MangoJobWorkerCapabilityEntity::getServiceCode, ownerService(definition))
                                .eq(MangoJobWorkerCapabilityEntity::getWorkerGroup, workerGroup(definition))
                                .eq(MangoJobWorkerCapabilityEntity::getAppCode, definition.getAppCode())
                                .eq(MangoJobWorkerCapabilityEntity::getHandlerName, definition.getHandlerName())
                                .and(wrapper -> wrapper
                                        .isNull(MangoJobWorkerCapabilityEntity::getJobCode)
                                        .or()
                                        .eq(MangoJobWorkerCapabilityEntity::getJobCode, "")
                                        .or()
                                        .eq(MangoJobWorkerCapabilityEntity::getJobCode, definition.getJobCode()))
                                .eq(MangoJobWorkerCapabilityEntity::getEnabled, 1))
                .stream()
                .map(MangoJobWorkerCapabilityEntity::getWorkerId)
                .collect(Collectors.toSet());
        Require.isTrue(!workerIds.isEmpty(), "未找到可执行任务的 Worker 能力："
                + ownerService(definition) + "/" + workerGroup(definition) + "/"
                + definition.getAppCode() + "/" + definition.getHandlerName() + "/" + definition.getJobCode());
        List<MangoJobWorkerSnapshotEntity> workers = workerSnapshotMapper.selectList(
                new LambdaQueryWrapper<MangoJobWorkerSnapshotEntity>()
                        .eq(MangoJobWorkerSnapshotEntity::getTenantId, definition.getTenantId())
                        .eq(MangoJobWorkerSnapshotEntity::getServiceCode, ownerService(definition))
                        .eq(MangoJobWorkerSnapshotEntity::getWorkerGroup, workerGroup(definition))
                        .eq(MangoJobWorkerSnapshotEntity::getEngineType, JobEngineType.MANGO_NATIVE.name())
                        .eq(MangoJobWorkerSnapshotEntity::getStatus, JobWorkerStatus.ONLINE.name())
                        .in(MangoJobWorkerSnapshotEntity::getId, workerIds)
                        .orderByDesc(MangoJobWorkerSnapshotEntity::getLastHeartbeatAt));
        MangoJobWorkerSnapshotEntity worker = workers.stream()
                .filter(this::canDispatchFromCurrentRuntime)
                .findFirst()
                .orElse(null);
        Require.notNull(worker, "未找到可执行任务的在线 Worker："
                + ownerService(definition) + "/" + workerGroup(definition) + "/"
                + definition.getAppCode() + "/" + definition.getHandlerName());
        return worker;
    }

    private boolean canDispatchFromCurrentRuntime(MangoJobWorkerSnapshotEntity worker) {
        JobTransportType transportType = resolveTransportType(worker);
        return transportType != JobTransportType.IN_MEMORY || workerAddress.equals(worker.getWorkerAddress());
    }

    private JobTransportType resolveTransportType(MangoJobWorkerSnapshotEntity worker) {
        if (StringUtils.hasText(worker.getTransportType())) {
            return JobTransportType.valueOf(worker.getTransportType());
        }
        String address = worker.getWorkerAddress();
        Require.notBlank(address, "Worker 地址不能为空");
        if (MangoJobTransportAddresses.isEmbedded(address)) {
            return JobTransportType.IN_MEMORY;
        }
        if (MangoJobTransportAddresses.isHttpInternal(address)) {
            return JobTransportType.HTTP_INTERNAL;
        }
        return properties.getTransport();
    }

    private MangoJobWorkerExecuteCommand toWorkerCommand(MangoJobDefinitionEntity definition,
                                                        MangoJobInstanceEntity instance,
                                                        String paramValue) {
        MangoJobWorkerExecuteCommand command = new MangoJobWorkerExecuteCommand();
        command.setTenantId(definition.getTenantId());
        command.setAppCode(definition.getAppCode());
        command.setOwnerService(ownerService(definition));
        command.setWorkerGroup(workerGroup(definition));
        command.setJobCode(definition.getJobCode());
        command.setHandlerName(definition.getHandlerName());
        command.setInstanceId(instance.getId());
        command.setOperatorId(instance.getTriggerUserId());
        command.setTriggerType(JobTriggerType.valueOf(instance.getTriggerType()));
        command.setTriggerBatchNo(instance.getTriggerBatchNo());
        command.setTraceId(instance.getTraceId());
        command.setParameter(paramValue);
        return command;
    }

    private boolean shouldSchedule(MangoJobDefinitionEntity definition) {
        return definition != null
                && JobEngineType.MANGO_NATIVE.name().equals(definition.getEngineType())
                && JobDefinitionStatus.ENABLED.name().equals(definition.getStatus())
                && !JobScheduleType.MANUAL.name().equals(definition.getScheduleType());
    }

    private boolean canHeartbeatSetOnline(MangoJobWorkerSnapshotEntity worker) {
        if (worker == null || !StringUtils.hasText(worker.getStatus())) {
            return true;
        }
        JobWorkerStatus status;
        try {
            status = JobWorkerStatus.valueOf(worker.getStatus());
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return status == JobWorkerStatus.REGISTERED
                || status == JobWorkerStatus.ONLINE
                || status == JobWorkerStatus.EXPIRED
                || status == JobWorkerStatus.UNKNOWN;
    }

    private void fillInstanceSnapshot(MangoJobDefinitionEntity definition,
                                      MangoJobInstanceEntity instance,
                                      JobTriggerType triggerType,
                                      LocalDateTime now,
                                      String idempotencyKey) {
        instance.setTenantId(definition.getTenantId());
        instance.setJobId(definition.getId());
        instance.setJobCode(definition.getJobCode());
        instance.setJobNameSnapshot(definition.getJobName());
        instance.setTriggerType(triggerType.name());
        instance.setTriggerTime(now);
        instance.setScheduledFireTime(triggerType == JobTriggerType.MANUAL ? now : instance.getScheduledFireTime());
        instance.setActualFireTime(now);
        instance.setStatus(JobInstanceStatus.WAITING.name());
        instance.setAttemptCount(instance.getAttemptCount() == null ? 0 : instance.getAttemptCount());
        instance.setEngineType(JobEngineType.MANGO_NATIVE.name());
        instance.setEngineInstanceId(String.valueOf(instance.getId()));
        instance.setIdempotencyKey(idempotencyKey);
        if (!StringUtils.hasText(instance.getTraceId())) {
            instance.setTraceId(UUID.randomUUID().toString());
        }
    }

    private void upsertLogIndex(MangoJobDefinitionEntity definition, MangoJobInstanceEntity instance) {
        MangoJobLogIndexEntity log = logIndexMapper.selectOne(new LambdaQueryWrapper<MangoJobLogIndexEntity>()
                .eq(MangoJobLogIndexEntity::getTenantId, definition.getTenantId())
                .eq(MangoJobLogIndexEntity::getInstanceId, instance.getId())
                .last("limit 1"));
        if (log == null) {
            log = new MangoJobLogIndexEntity();
            log.setTenantId(definition.getTenantId());
            log.setJobId(definition.getId());
            log.setInstanceId(instance.getId());
            log.setEngineType(JobEngineType.MANGO_NATIVE.name());
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

    private String formatLogChunk(MangoJobLogChunkEntity chunk) {
        return "[" + chunk.getLogTime() + "]"
                + "[" + chunk.getLevel() + "]"
                + "[" + chunk.getLoggerName() + "] "
                + chunk.getContent();
    }

    private String resolveParam(MangoJobDefinitionEntity definition, String paramValue) {
        return StringUtils.hasText(paramValue) ? paramValue : definition.getParamValue();
    }

    private String ownerService(MangoJobDefinitionEntity definition) {
        if (StringUtils.hasText(definition.getOwnerService())) {
            return definition.getOwnerService();
        }
        return definition.getAppCode();
    }

    private String workerGroup(MangoJobDefinitionEntity definition) {
        if (StringUtils.hasText(definition.getWorkerGroup())) {
            return definition.getWorkerGroup();
        }
        return ownerService(definition);
    }

    private String workerRegistrationKey(MangoJobHandlerVO handler) {
        return handler.getAppCode() + ":" + handler.getServiceCode() + ":" + handler.getWorkerGroup();
    }

    private String currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, "缺少当前租户上下文");
        return tenantId;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String resolveWorkerAddress(Environment environment) {
        if (StringUtils.hasText(properties.getWorkerAddress())) {
            return properties.getWorkerAddress().trim();
        }
        return MangoJobTransportAddresses.EMBEDDED_PREFIX + localIp() + portSuffix(environment);
    }

    private String resolveWorkerInstanceId(Environment environment, String address) {
        String serviceName = environment.getProperty("spring.application.name");
        if (!StringUtils.hasText(serviceName)) {
            serviceName = "mango-job";
        }
        return serviceName.trim() + "@" + addressIdentity(address)
                + "#" + ManagementFactory.getRuntimeMXBean().getName();
    }

    private String addressIdentity(String address) {
        if (!StringUtils.hasText(address)) {
            return "unknown";
        }
        if (address.startsWith(MangoJobTransportAddresses.EMBEDDED_PREFIX)) {
            return address.substring(MangoJobTransportAddresses.EMBEDDED_PREFIX.length());
        }
        if (address.startsWith(MangoJobTransportAddresses.IN_MEMORY_PREFIX)) {
            return address.substring(MangoJobTransportAddresses.IN_MEMORY_PREFIX.length());
        }
        if (address.startsWith(MangoJobTransportAddresses.HTTP_PREFIX)) {
            return address.substring(MangoJobTransportAddresses.HTTP_PREFIX.length());
        }
        if (address.startsWith(MangoJobTransportAddresses.HTTPS_PREFIX)) {
            return address.substring(MangoJobTransportAddresses.HTTPS_PREFIX.length());
        }
        return address;
    }

    private String portSuffix(Environment environment) {
        String port = firstText(environment.getProperty("local.server.port"), environment.getProperty("server.port"));
        if (!StringUtils.hasText(port) || "0".equals(port.trim())) {
            return ":8080";
        }
        return ":" + port.trim();
    }

    private String localIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            return "127.0.0.1";
        }
    }
}
