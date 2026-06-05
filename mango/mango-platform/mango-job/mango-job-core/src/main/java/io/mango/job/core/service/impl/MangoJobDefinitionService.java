package io.mango.job.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.job.api.command.SaveMangoJobDefinitionCommand;
import io.mango.job.api.command.TriggerMangoJobCommand;
import io.mango.job.api.command.UpdateMangoJobDefinitionStatusCommand;
import io.mango.job.api.enums.JobDefinitionStatus;
import io.mango.job.api.enums.JobInstanceStatus;
import io.mango.job.api.enums.JobScheduleType;
import io.mango.job.api.enums.JobSyncStatus;
import io.mango.job.api.enums.JobTriggerType;
import io.mango.job.api.enums.JobType;
import io.mango.job.api.query.MangoJobDefinitionPageQuery;
import io.mango.job.api.vo.MangoJobDefinitionVO;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobInstanceEntity;
import io.mango.job.core.entity.MangoJobLogIndexEntity;
import io.mango.job.core.entity.MangoJobOperationLogEntity;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.job.core.mapper.MangoJobInstanceMapper;
import io.mango.job.core.mapper.MangoJobLogIndexMapper;
import io.mango.job.core.mapper.MangoJobOperationLogMapper;
import io.mango.job.core.service.IMangoJobDefinitionService;
import io.mango.job.core.service.engine.IMangoJobEngineSyncService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Job 任务定义内部服务实现。
 */
@Service
public class MangoJobDefinitionService implements IMangoJobDefinitionService {

    private final MangoJobDefinitionMapper mapper;

    private final MangoJobInstanceMapper instanceMapper;

    private final MangoJobOperationLogMapper operationLogMapper;

    private final MangoJobLogIndexMapper logIndexMapper;

    private final MangoJobDataSourceRouter dataSourceRouter;

    private final IMangoJobEngineSyncService engineSyncService;

    public MangoJobDefinitionService(MangoJobDefinitionMapper mapper,
                                     MangoJobInstanceMapper instanceMapper,
                                     MangoJobOperationLogMapper operationLogMapper,
                                     MangoJobLogIndexMapper logIndexMapper,
                                     MangoJobDataSourceRouter dataSourceRouter,
                                     IMangoJobEngineSyncService engineSyncService) {
        this.mapper = mapper;
        this.instanceMapper = instanceMapper;
        this.operationLogMapper = operationLogMapper;
        this.logIndexMapper = logIndexMapper;
        this.dataSourceRouter = dataSourceRouter;
        this.engineSyncService = engineSyncService;
    }

    @Override
    public PageResult<MangoJobDefinitionVO> pageDefinitions(MangoJobDefinitionPageQuery query) {
        return dataSourceRouter.route(() -> {
            MangoJobDefinitionPageQuery resolved = query == null ? new MangoJobDefinitionPageQuery() : query;
            IPage<MangoJobDefinitionEntity> page = mapper.selectPage(
                    new Page<>(resolved.getPage(), resolved.getSize()),
                    definitionWrapper(resolved));
            List<MangoJobDefinitionVO> records = page.getRecords().stream()
                    .map(MangoJobSupport::toDefinitionVO)
                    .toList();
            return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
        });
    }

    @Override
    public MangoJobDefinitionVO detailDefinition(Long id) {
        return dataSourceRouter.route(() -> MangoJobSupport.toDefinitionVO(selectDefinitionRequired(id)));
    }

    @Override
    public Long createDefinition(SaveMangoJobDefinitionCommand command) {
        return dataSourceRouter.route(() -> {
            Require.notNull(command, "任务定义不能为空");
            validateDefinition(command, false);
            String tenantId = MangoJobSupport.currentTenantId();
            Require.isNull(selectByCode(tenantId, command.getAppCode(), command.getJobCode()), "任务编码已存在");

            MangoJobDefinitionEntity entity = new MangoJobDefinitionEntity();
            copyDefinition(command, entity);
            entity.setTenantId(tenantId);
            entity.setStatus(JobDefinitionStatus.DRAFT.name());
            entity.setSyncStatus(MangoJobSupport.syncStatus(JobSyncStatus.PENDING));
            mapper.insert(entity);
            engineSyncService.syncDefinition(entity, "CREATE");
            writeOperationLog(entity, null, "CREATE_DEFINITION", "SUCCESS", command.getJobCode(), null);
            return entity.getId();
        });
    }

    @Override
    public Boolean updateDefinition(SaveMangoJobDefinitionCommand command) {
        return dataSourceRouter.route(() -> {
            Require.notNull(command, "任务定义不能为空");
            Require.notNull(command.getId(), "任务 ID 不能为空");
            validateDefinition(command, true);
            MangoJobDefinitionEntity entity = selectDefinitionRequired(command.getId());
            Require.isTrue(JobDefinitionStatus.DRAFT.name().equals(entity.getStatus()), "只有草稿任务可以编辑");
            MangoJobDefinitionEntity exists = selectByCode(entity.getTenantId(), command.getAppCode(), command.getJobCode());
            Require.isTrue(exists == null || exists.getId().equals(entity.getId()), "任务编码已存在");

            copyDefinition(command, entity);
            entity.setSyncStatus(MangoJobSupport.syncStatus(JobSyncStatus.PENDING));
            boolean updated = mapper.updateById(entity) > 0;
            engineSyncService.syncDefinition(entity, "UPDATE");
            writeOperationLog(entity, null, "UPDATE_DEFINITION", "SUCCESS", command.getJobCode(), null);
            return updated;
        });
    }

    @Override
    public Boolean updateDefinitionStatus(UpdateMangoJobDefinitionStatusCommand command) {
        return dataSourceRouter.route(() -> {
            Require.notNull(command, "状态命令不能为空");
            Require.notNull(command.getId(), "任务 ID 不能为空");
            JobDefinitionStatus target = MangoJobSupport.definitionStatus(command.getStatus());
            MangoJobDefinitionEntity entity = selectDefinitionRequired(command.getId());
            validateStatusTransition(entity.getStatus(), target);
            entity.setStatus(target.name());
            entity.setSyncStatus(MangoJobSupport.syncStatus(JobSyncStatus.PENDING));
            boolean updated = mapper.updateById(entity) > 0;
            engineSyncService.syncDefinition(entity, "UPDATE_STATUS");
            writeOperationLog(entity, null, "UPDATE_STATUS", "SUCCESS", target.name(), null);
            return updated;
        });
    }

    @Override
    public Boolean deleteDefinition(Long id) {
        return dataSourceRouter.route(() -> {
            MangoJobDefinitionEntity entity = selectDefinitionRequired(id);
            Require.isTrue(JobDefinitionStatus.DRAFT.name().equals(entity.getStatus()), "只有草稿任务可以删除");
            engineSyncService.deleteDefinition(entity);
            boolean deleted = mapper.deleteById(id) > 0;
            writeOperationLog(entity, null, "DELETE_DEFINITION", "SUCCESS", entity.getJobCode(), null);
            return deleted;
        });
    }

    @Override
    public Long triggerDefinition(TriggerMangoJobCommand command) {
        return dataSourceRouter.route(() -> {
            Require.notNull(command, "触发命令不能为空");
            Require.notNull(command.getJobId(), "任务 ID 不能为空");
            MangoJobDefinitionEntity definition = selectDefinitionRequired(command.getJobId());
            Require.isTrue(!JobDefinitionStatus.DRAFT.name().equals(definition.getStatus()), "草稿任务不能触发");
            Require.isTrue(!JobDefinitionStatus.DISABLED.name().equals(definition.getStatus()), "已禁用任务不能触发");

            String batchNo = StringUtils.hasText(command.getTriggerBatchNo())
                    ? command.getTriggerBatchNo().trim()
                    : UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            MangoJobInstanceEntity instance = new MangoJobInstanceEntity();
            instance.setTenantId(definition.getTenantId());
            instance.setJobId(definition.getId());
            instance.setTriggerType(MangoJobSupport.triggerType(JobTriggerType.MANUAL));
            instance.setTriggerUserId(MangoContextHolder.userId());
            instance.setTriggerTime(now);
            instance.setStatus(MangoJobSupport.instanceStatus(JobInstanceStatus.WAITING));
            instance.setEngineType(definition.getEngineType());
            instance.setTraceId(MangoContextHolder.traceId());
            instance.setTriggerBatchNo(batchNo);
            instanceMapper.insert(instance);

            engineSyncService.trigger(definition, instance, batchNo);
            writeExecutionLogIndex(definition, instance);
            writeOperationLog(definition, instance.getId(), "TRIGGER_DEFINITION", "SUCCESS", batchNo, null);
            return instance.getId();
        });
    }

    @Override
    public MangoJobDefinitionEntity saveDefinition(MangoJobDefinitionEntity entity) {
        return dataSourceRouter.route(() -> {
            mapper.insert(entity);
            return entity;
        });
    }

    @Override
    public MangoJobDefinitionEntity findById(Long id) {
        return dataSourceRouter.route(() -> mapper.selectById(id));
    }

    private LambdaQueryWrapper<MangoJobDefinitionEntity> definitionWrapper(MangoJobDefinitionPageQuery query) {
        String tenantId = MangoJobSupport.currentTenantId();
        String keyword = MangoJobSupport.trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<MangoJobDefinitionEntity>()
                .eq(MangoJobDefinitionEntity::getTenantId, tenantId)
                .eq(StringUtils.hasText(query.getAppCode()), MangoJobDefinitionEntity::getAppCode, query.getAppCode())
                .eq(StringUtils.hasText(query.getStatus()), MangoJobDefinitionEntity::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getJobType()), MangoJobDefinitionEntity::getJobType, query.getJobType())
                .eq(StringUtils.hasText(query.getScheduleType()), MangoJobDefinitionEntity::getScheduleType, query.getScheduleType())
                .eq(StringUtils.hasText(query.getEngineType()), MangoJobDefinitionEntity::getEngineType, query.getEngineType())
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(MangoJobDefinitionEntity::getJobCode, keyword)
                        .or()
                        .like(MangoJobDefinitionEntity::getJobName, keyword)
                        .or()
                        .like(MangoJobDefinitionEntity::getHandlerName, keyword))
                .orderByDesc(MangoJobDefinitionEntity::getUpdatedAt);
    }

    private void validateDefinition(SaveMangoJobDefinitionCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), "任务 ID 不能为空");
        }
        JobType jobType = MangoJobSupport.jobType(command.getJobType());
        JobScheduleType scheduleType = MangoJobSupport.scheduleType(command.getScheduleType());
        MangoJobSupport.engineType(command.getEngineType());
        if (jobType == JobType.SCRIPT) {
            Require.fail(400, "脚本任务首轮不启用");
        }
        if (jobType == JobType.BUILTIN || jobType == JobType.REMOTE_API) {
            Require.notBlank(command.getHandlerName(), "处理器名称不能为空");
        }
        if (scheduleType != JobScheduleType.MANUAL) {
            Require.notBlank(command.getScheduleExpression(), "调度表达式不能为空");
        }
        if (command.getTimeoutSeconds() != null) {
            Require.isTrue(command.getTimeoutSeconds() > 0, "执行超时必须大于0");
        }
    }

    private void validateStatusTransition(String currentStatus, JobDefinitionStatus target) {
        JobDefinitionStatus current = MangoJobSupport.definitionStatus(currentStatus);
        Require.isTrue(current != target, "任务已经是目标状态");
        if (current == JobDefinitionStatus.DRAFT) {
            Require.isTrue(target == JobDefinitionStatus.ENABLED || target == JobDefinitionStatus.DISABLED,
                    "草稿任务只能启用或禁用");
            return;
        }
        if (current == JobDefinitionStatus.DISABLED) {
            Require.isTrue(target == JobDefinitionStatus.ENABLED || target == JobDefinitionStatus.DRAFT,
                    "禁用任务只能启用或退回草稿");
            return;
        }
        if (current == JobDefinitionStatus.ENABLED) {
            Require.isTrue(target == JobDefinitionStatus.PAUSED || target == JobDefinitionStatus.DISABLED,
                    "启用任务只能暂停或禁用");
            return;
        }
        Require.isTrue(target == JobDefinitionStatus.ENABLED || target == JobDefinitionStatus.DISABLED,
                "暂停任务只能启用或禁用");
    }

    private MangoJobDefinitionEntity selectDefinitionRequired(Long id) {
        Require.notNull(id, "任务 ID 不能为空");
        MangoJobDefinitionEntity entity = mapper.selectById(id);
        Require.notNull(entity, "任务不存在");
        Require.isTrue(MangoJobSupport.currentTenantId().equals(entity.getTenantId()), "任务不存在");
        return entity;
    }

    private MangoJobDefinitionEntity selectByCode(String tenantId, String appCode, String jobCode) {
        return mapper.selectOne(new LambdaQueryWrapper<MangoJobDefinitionEntity>()
                .eq(MangoJobDefinitionEntity::getTenantId, tenantId)
                .eq(MangoJobDefinitionEntity::getAppCode, appCode.trim())
                .eq(MangoJobDefinitionEntity::getJobCode, jobCode.trim()));
    }

    private void copyDefinition(SaveMangoJobDefinitionCommand command, MangoJobDefinitionEntity entity) {
        entity.setAppCode(MangoJobSupport.normalizeRequired(command.getAppCode(), "所属应用不能为空"));
        entity.setJobCode(MangoJobSupport.normalizeRequired(command.getJobCode(), "任务编码不能为空"));
        entity.setJobName(MangoJobSupport.normalizeRequired(command.getJobName(), "任务名称不能为空"));
        entity.setJobType(MangoJobSupport.jobType(command.getJobType()).name());
        entity.setScheduleType(MangoJobSupport.scheduleType(command.getScheduleType()).name());
        entity.setScheduleExpression(MangoJobSupport.trimToNull(command.getScheduleExpression()));
        entity.setHandlerName(MangoJobSupport.trimToNull(command.getHandlerName()));
        entity.setParamSchema(MangoJobSupport.trimToNull(command.getParamSchema()));
        entity.setParamValue(MangoJobSupport.trimToNull(command.getParamValue()));
        entity.setMisfireStrategy(MangoJobSupport.trimToNull(command.getMisfireStrategy()));
        entity.setConcurrencyPolicy(MangoJobSupport.trimToNull(command.getConcurrencyPolicy()));
        entity.setTimeoutSeconds(command.getTimeoutSeconds());
        entity.setRetryPolicy(MangoJobSupport.trimToNull(command.getRetryPolicy()));
        entity.setEngineType(MangoJobSupport.engineType(command.getEngineType()).name());
    }

    private void writeOperationLog(MangoJobDefinitionEntity definition,
                                   Long instanceId,
                                   String operationType,
                                   String resultStatus,
                                   String requestSummary,
                                   String errorSummary) {
        MangoJobOperationLogEntity log = new MangoJobOperationLogEntity();
        log.setTenantId(definition.getTenantId());
        log.setJobId(definition.getId());
        log.setInstanceId(instanceId);
        log.setOperationType(operationType);
        log.setOperatorId(MangoContextHolder.userId());
        log.setOperatorName(MangoContextHolder.principalName());
        log.setRequestSummary(requestSummary);
        log.setResultStatus(resultStatus);
        log.setErrorSummary(errorSummary);
        log.setTraceId(MangoContextHolder.traceId());
        operationLogMapper.insert(log);
    }

    private void writeExecutionLogIndex(MangoJobDefinitionEntity definition, MangoJobInstanceEntity instance) {
        MangoJobLogIndexEntity log = new MangoJobLogIndexEntity();
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
    }
}
