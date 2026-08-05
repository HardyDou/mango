package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.command.StartBusinessWorkflowCommand;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.command.WithdrawWorkflowProcessCommand;
import io.mango.workflow.api.command.WorkflowJsonRequest;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowDefinitionStatus;
import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.mango.workflow.api.enums.WorkflowInstanceStatus;
import io.mango.workflow.api.enums.WorkflowTaskAction;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessApplyCurrentTaskVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessProcessVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import io.mango.workflow.api.vo.WorkflowProcessWithdrawResultVO;
import io.mango.workflow.api.vo.WorkflowStartResultVO;
import io.mango.workflow.core.entity.WorkflowDefinitionEntity;
import io.mango.workflow.core.entity.WorkflowFormInstanceEntity;
import io.mango.workflow.core.entity.WorkflowTaskRecordEntity;
import io.mango.workflow.core.event.WorkflowEventPublisher;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import io.mango.workflow.core.model.WorkflowProcessStartedContext;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import io.mango.workflow.core.service.IWorkflowProcessService;
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 流程实例服务实现。
 */
@Service
@RequiredArgsConstructor
public class WorkflowProcessService implements IWorkflowProcessService {

    private static final String INITIATOR_VAR = "mangoInitiator";
    private static final String INITIATOR_NAME_VAR = "mangoInitiatorName";
    private static final String DEFINITION_ID_VAR = "mangoDefinitionId";
    private static final String DEFINITION_ADMIN_USERS_VAR = "mangoDefinitionAdminUsers";
    private static final String BUSINESS_TYPE_VAR = "businessType";
    private static final String BUSINESS_KEY_VAR = "businessKey";
    private static final String APPLY_ID_VAR = "applyId";
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final WorkflowDefinitionMapper definitionMapper;
    private final WorkflowFormInstanceMapper formInstanceMapper;
    private final WorkflowTaskRecordMapper taskRecordMapper;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final ObjectMapper objectMapper;
    private final IWorkflowTaskRuntimeService workflowTaskRuntimeService;
    private final IWorkflowBusinessApplyService workflowBusinessApplyService;
    private final WorkflowEventPublisher workflowEventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowProcessInstanceVO start(StartWorkflowProcessCommand command) {
        Require.notNull(command, WorkflowCode.DEFINITION_INVALID);
        WorkflowDefinitionEntity definition = selectDefinition(command);
        Require.notNull(definition, WorkflowCode.DEFINITION_NOT_FOUND);
        Require.isTrue(WorkflowDefinitionStatus.PUBLISHED.name().equals(definition.getStatus()),
                WorkflowCode.DEFINITION_STATUS_INVALID, "只有已发布流程可以发起");
        Require.notBlank(definition.getProcessDefinitionId(), WorkflowCode.DEFINITION_STATUS_INVALID,
                "流程未部署到引擎，请先发布");

        Map<String, Object> variables = new HashMap<>();
        if (command.getVariables() != null) {
            variables.putAll(command.getVariables().toMap());
        }
        if (StringUtils.hasText(command.getBusinessType())) {
            variables.put(BUSINESS_TYPE_VAR, command.getBusinessType().trim());
        }
        if (command.getApplyId() != null) {
            variables.put(APPLY_ID_VAR, String.valueOf(command.getApplyId()));
        }
        if (command.getSelectedAssignees() != null && !command.getSelectedAssignees().isEmpty()) {
            variables.put("mangoSelectedAssignees", command.getSelectedAssignees().toMap());
        }
        String initiator = currentUser();
        variables.put(INITIATOR_VAR, initiator);
        variables.put(INITIATOR_NAME_VAR, initiator);
        variables.put(DEFINITION_ID_VAR, String.valueOf(definition.getId()));
        variables.put(DEFINITION_ADMIN_USERS_VAR, parseAdminUsers(definition.getAdminUsers()));

        String businessKey = StringUtils.hasText(command.getBusinessKey())
                ? command.getBusinessKey().trim()
                : definition.getDefinitionKey() + "-" + System.currentTimeMillis();
        variables.put(BUSINESS_KEY_VAR, businessKey);
        Long applyId = resolveApplyId(command, definition, businessKey, variables);
        if (applyId != null) {
            variables.put(APPLY_ID_VAR, String.valueOf(applyId));
        }
        ProcessInstance instance = runtimeService.startProcessInstanceById(
                definition.getProcessDefinitionId(),
                businessKey,
                variables);
        saveFormInstance(definition, instance, variables);
        saveStartRecord(instance.getProcessInstanceId(), variables);
        workflowEventPublisher.publishProcessStarted(definition, instance, variables);
        workflowBusinessApplyService.markProcessStarted(new WorkflowProcessStartedContext(
                applyId, definition.getId(), definition.getDefinitionKey(),
                definition.getProcessDefinitionId(), definition.getDefinitionName(), instance.getProcessInstanceId()));
        workflowTaskRuntimeService.advanceRuntimeTasks(instance.getProcessInstanceId());
        boolean ended = isProcessEnded(instance.getProcessInstanceId());
        if (ended) {
            WorkflowFormInstanceEntity formInstance = formInstanceMapper.selectOne(new LambdaQueryWrapper<WorkflowFormInstanceEntity>()
                    .eq(WorkflowFormInstanceEntity::getProcessInstanceId, instance.getProcessInstanceId())
                    .last("limit 1"));
            updateCompletedFormInstance(formInstance);
            workflowBusinessApplyService.markApproved(instance.getProcessInstanceId());
            WorkflowBusinessApplyVO apply = workflowBusinessApplyService.findByProcessInstance(instance.getProcessInstanceId());
            workflowEventPublisher.publishProcessCompleted(instance.getProcessInstanceId(), formInstance, variables, apply);
        }

        WorkflowProcessInstanceVO vo = new WorkflowProcessInstanceVO();
        vo.setProcessInstanceId(instance.getProcessInstanceId());
        vo.setBusinessKey(instance.getBusinessKey());
        vo.setApplyId(applyId);
        vo.setDefinitionId(definition.getId());
        vo.setProcessName(definition.getDefinitionName());
        vo.setProcessKey(definition.getDefinitionKey());
        vo.setProcessDefinitionId(definition.getProcessDefinitionId());
        vo.setInitiatorName(initiator);
        fillCurrentTask(instance.getProcessInstanceId(), vo);
        vo.setStatus(ended ? WorkflowInstanceStatus.COMPLETED.getLabel() : WorkflowInstanceStatus.RUNNING.getLabel());
        vo.setStartTime(LocalDateTime.now());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowStartResultVO startBusinessWorkflow(StartBusinessWorkflowCommand command) {
        Require.notNull(command, WorkflowCode.DEFINITION_INVALID);
        Require.notBlank(command.getBusinessType(), WorkflowCode.APPLY_INVALID, "业务类型不能为空");
        Require.notBlank(command.getBusinessKey(), WorkflowCode.APPLY_INVALID, "业务主键不能为空");
        Require.notBlank(command.getApplyTitle(), WorkflowCode.APPLY_INVALID, "申请标题不能为空");
        WorkflowDefinitionEntity definition = selectDefinition(toStartCommand(command, null));
        Require.notNull(definition, WorkflowCode.DEFINITION_NOT_FOUND);

        CreateWorkflowBusinessApplyCommand applyCommand = toCreateApplyCommand(command, definition);
        WorkflowBusinessApplyVO apply = workflowBusinessApplyService.create(applyCommand);
        Require.notNull(apply, WorkflowCode.APPLY_INVALID, "业务申请创建失败");

        StartWorkflowProcessCommand startCommand = toStartCommand(command, apply.getId());
        WorkflowProcessInstanceVO process = start(startCommand);
        WorkflowBusinessApplyProgressVO progress = workflowBusinessApplyService
                .latestProgress(command.getBusinessType(), command.getBusinessKey());
        return toStartResult(process, progress);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowProcessWithdrawResultVO withdraw(WithdrawWorkflowProcessCommand command) {
        Require.notNull(command, WorkflowCode.APPLY_INVALID);
        Require.isTrue(command.getApplyId() != null || StringUtils.hasText(command.getProcessInstanceId()),
                WorkflowCode.APPLY_INVALID, "业务申请ID和流程实例ID不能同时为空");
        Require.notBlank(command.getReason(), WorkflowCode.APPLY_INVALID, "撤回原因不能为空");
        Require.notBlank(MangoContextHolder.tenantId(), WorkflowCode.PROCESS_WITHDRAW_FORBIDDEN,
                "缺少当前租户上下文");
        Require.notNull(MangoContextHolder.userId(), WorkflowCode.PROCESS_WITHDRAW_FORBIDDEN,
                "缺少当前用户上下文");

        String requestedProcessInstanceId = trim(command.getProcessInstanceId());
        WorkflowBusinessApplyVO apply = workflowBusinessApplyService.lockWithdrawalTarget(
                command.getApplyId(), requestedProcessInstanceId);
        Require.notNull(apply, WorkflowCode.APPLY_NOT_FOUND);
        Require.isTrue(Objects.equals(MangoContextHolder.userId(), apply.getApplicantId()),
                WorkflowCode.PROCESS_WITHDRAW_FORBIDDEN);
        if (command.getApplyId() != null && requestedProcessInstanceId != null) {
            Require.isTrue(Objects.equals(command.getApplyId(), apply.getId())
                            && requestedProcessInstanceId.equals(apply.getProcessInstanceId()),
                    WorkflowCode.APPLY_INVALID, "业务申请ID与流程实例ID不匹配");
        }

        WorkflowApplyStatus previousStatus = apply.getApplyStatus();
        Require.notNull(previousStatus, WorkflowCode.PROCESS_WITHDRAW_NOT_ALLOWED, "业务申请状态无效");
        String reason = command.getReason().trim();
        if (previousStatus == WorkflowApplyStatus.WITHDRAWN) {
            return toWithdrawResult(apply, previousStatus, reason, true);
        }
        Require.isTrue(previousStatus == WorkflowApplyStatus.IN_APPROVAL,
                WorkflowCode.PROCESS_WITHDRAW_NOT_ALLOWED,
                "当前申请状态为" + previousStatus.getLabel() + "，不能撤回");
        Require.notBlank(apply.getProcessInstanceId(), WorkflowCode.PROCESS_INSTANCE_NOT_FOUND,
                "业务申请未关联流程实例");

        String processInstanceId = apply.getProcessInstanceId();
        ProcessInstance runningInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Require.notNull(runningInstance, WorkflowCode.PROCESS_INSTANCE_NOT_FOUND,
                "运行中的流程实例不存在");

        WorkflowFormInstanceEntity formInstance = findFormInstance(processInstanceId);
        Map<String, Object> variables = withdrawalVariables(formInstance, apply, processInstanceId);
        runtimeService.deleteProcessInstance(processInstanceId, reason);
        markFormInstanceWithdrawn(formInstance, variables);
        WorkflowBusinessApplyVO withdrawnApply = workflowBusinessApplyService.markWithdrawn(processInstanceId, reason);
        workflowEventPublisher.publishProcessWithdrawn(
                processInstanceId, formInstance, variables, reason, withdrawnApply);
        workflowEventPublisher.publishProcessEnded(
                processInstanceId, formInstance, variables, reason, withdrawnApply);
        return toWithdrawResult(withdrawnApply, previousStatus, reason, false);
    }

    private WorkflowDefinitionEntity selectDefinition(StartWorkflowProcessCommand command) {
        if (command.getDefinitionId() != null) {
            return definitionMapper.selectById(command.getDefinitionId());
        }
        Require.notBlank(command.getDefinitionKey(), WorkflowCode.DEFINITION_INVALID,
                "流程定义ID和流程定义编码不能同时为空");
        return definitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                .eq(WorkflowDefinitionEntity::getDefinitionKey, command.getDefinitionKey().trim())
                .eq(WorkflowDefinitionEntity::getStatus, WorkflowDefinitionStatus.PUBLISHED.name())
                .orderByDesc(WorkflowDefinitionEntity::getPublishedVersionNo)
                .orderByDesc(WorkflowDefinitionEntity::getUpdatedAt)
                .last("limit 1"));
    }

    private CreateWorkflowBusinessApplyCommand toCreateApplyCommand(StartBusinessWorkflowCommand command,
                                                                    WorkflowDefinitionEntity definition) {
        CreateWorkflowBusinessApplyCommand applyCommand = new CreateWorkflowBusinessApplyCommand();
        applyCommand.setApplyCode(trim(command.getApplyCode()));
        applyCommand.setBusinessType(command.getBusinessType().trim());
        applyCommand.setBusinessKey(command.getBusinessKey().trim());
        applyCommand.setApplyTitle(command.getApplyTitle().trim());
        applyCommand.setApplySummary(trim(command.getApplySummary()));
        applyCommand.setProcessDefinitionId(definition.getId());
        applyCommand.setProcessDefinitionKey(definition.getDefinitionKey());
        applyCommand.setRenderMode(command.getRenderMode() == null
                ? resolveRenderMode(definition)
                : command.getRenderMode());
        applyCommand.setApplyPageKey(trim(command.getApplyPageKey()));
        applyCommand.setApprovePageKey(trim(command.getApprovePageKey()));
        applyCommand.setFormKey(StringUtils.hasText(command.getFormKey())
                ? command.getFormKey().trim()
                : definition.getFormCode());
        applyCommand.setFormVersion(command.getFormVersion() == null
                ? definition.getPublishedVersionNo()
                : command.getFormVersion());
        applyCommand.setFormJsonSnapshot(StringUtils.hasText(command.getFormJsonSnapshot())
                ? command.getFormJsonSnapshot()
                : definition.getFormJson());
        applyCommand.setFormDataSnapshot(command.getFormDataSnapshot());
        applyCommand.setSnapshotRef(trim(command.getSnapshotRef()));
        applyCommand.setSnapshotDigest(trim(command.getSnapshotDigest()));
        applyCommand.setVariables(command.getVariables());
        applyCommand.setExtension(command.getExtension());
        return applyCommand;
    }

    private StartWorkflowProcessCommand toStartCommand(StartBusinessWorkflowCommand command, Long applyId) {
        StartWorkflowProcessCommand startCommand = new StartWorkflowProcessCommand();
        startCommand.setDefinitionId(command.getDefinitionId());
        startCommand.setDefinitionKey(command.getDefinitionKey());
        startCommand.setBusinessType(command.getBusinessType());
        startCommand.setBusinessKey(command.getBusinessKey());
        startCommand.setApplyId(applyId);
        startCommand.setRenderMode(command.getRenderMode());
        startCommand.setApplyPageKey(command.getApplyPageKey());
        startCommand.setApprovePageKey(command.getApprovePageKey());
        startCommand.setSnapshotRef(command.getSnapshotRef());
        startCommand.setVariables(command.getVariables());
        startCommand.setSelectedAssignees(command.getSelectedAssignees());
        return startCommand;
    }

    private WorkflowStartResultVO toStartResult(WorkflowProcessInstanceVO process,
                                                WorkflowBusinessApplyProgressVO progress) {
        WorkflowStartResultVO vo = new WorkflowStartResultVO();
        if (process != null) {
            vo.setProcessInstanceId(process.getProcessInstanceId());
            vo.setApplyId(process.getApplyId());
            vo.setBusinessKey(process.getBusinessKey());
        }
        if (progress == null) {
            vo.setCurrentTasks(List.of());
            return vo;
        }
        vo.setApplyId(progress.getApplyId());
        vo.setProcessInstanceId(progress.getProcessInstanceId());
        vo.setBusinessType(progress.getBusinessType());
        vo.setBusinessKey(progress.getBusinessKey());
        vo.setProcessStatus(progress.getProcessStatus());
        vo.setProcessStatusName(progress.getProcessStatusName());
        vo.setCurrentTaskId(progress.getCurrentTaskId());
        vo.setCurrentTaskName(progress.getCurrentTaskName());
        vo.setTaskDefinitionKey(progress.getTaskDefinitionKey());
        vo.setAssigneeId(progress.getAssigneeId());
        vo.setAssigneeName(progress.getAssigneeName());
        vo.setClaimStatus(progress.getClaimStatus());
        vo.setCandidateUsers(progress.getCandidateUsers());
        vo.setCandidateGroups(progress.getCandidateGroups());
        vo.setCurrentTasks(progress.getCurrentTasks());
        return vo;
    }

    private boolean isProcessEnded(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult() == null;
    }

    private WorkflowFormInstanceEntity findFormInstance(String processInstanceId) {
        return formInstanceMapper.selectOne(new LambdaQueryWrapper<WorkflowFormInstanceEntity>()
                .eq(WorkflowFormInstanceEntity::getProcessInstanceId, processInstanceId)
                .last("limit 1"));
    }

    private Map<String, Object> withdrawalVariables(WorkflowFormInstanceEntity formInstance,
                                                     WorkflowBusinessApplyVO apply,
                                                     String processInstanceId) {
        Map<String, Object> variables = new LinkedHashMap<>();
        if (formInstance != null) {
            variables.putAll(parseMap(formInstance.getVariablesJson()));
        }
        if (variables.isEmpty()) {
            Map<String, Object> runtimeVariables = runtimeService.getVariables(processInstanceId);
            if (runtimeVariables != null) {
                variables.putAll(runtimeVariables);
            }
        }
        variables.putIfAbsent(BUSINESS_TYPE_VAR, apply.getBusinessType());
        variables.putIfAbsent(BUSINESS_KEY_VAR, apply.getBusinessKey());
        variables.putIfAbsent(APPLY_ID_VAR, String.valueOf(apply.getId()));
        return variables;
    }

    private void markFormInstanceWithdrawn(WorkflowFormInstanceEntity formInstance, Map<String, Object> variables) {
        if (formInstance == null || WorkflowInstanceStatus.WITHDRAWN.name().equals(formInstance.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        formInstance.setVariablesJson(toJson(variables));
        formInstance.setStatus(WorkflowInstanceStatus.WITHDRAWN.name());
        formInstance.setUpdatedBy(MangoContextHolder.userId());
        formInstance.setUpdatedTime(now);
        formInstance.setUpdatedAt(now);
        formInstanceMapper.updateById(formInstance);
    }

    private WorkflowProcessWithdrawResultVO toWithdrawResult(WorkflowBusinessApplyVO apply,
                                                              WorkflowApplyStatus previousStatus,
                                                              String reason,
                                                              boolean idempotent) {
        WorkflowProcessWithdrawResultVO result = new WorkflowProcessWithdrawResultVO();
        result.setApplyId(apply.getId());
        result.setProcessInstanceId(apply.getProcessInstanceId());
        result.setPreviousStatus(previousStatus);
        WorkflowApplyStatus currentStatus = idempotent ? WorkflowApplyStatus.WITHDRAWN : apply.getApplyStatus();
        result.setApplyStatus(currentStatus);
        result.setApplyStatusName(currentStatus == null ? null : currentStatus.getLabel());
        result.setWithdrawn(currentStatus == WorkflowApplyStatus.WITHDRAWN);
        result.setIdempotent(idempotent);
        result.setEnded(currentStatus == WorkflowApplyStatus.WITHDRAWN);
        result.setReason(reason);
        return result;
    }

    private void updateCompletedFormInstance(WorkflowFormInstanceEntity formInstance) {
        if (formInstance == null || WorkflowInstanceStatus.COMPLETED.name().equals(formInstance.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        formInstance.setStatus(WorkflowInstanceStatus.COMPLETED.name());
        formInstance.setUpdatedBy(MangoContextHolder.userId());
        formInstance.setUpdatedTime(now);
        formInstance.setUpdatedAt(now);
        formInstanceMapper.updateById(formInstance);
    }

    @Override
    public PageResult<WorkflowProcessInstanceVO> initiated(WorkflowTaskPageQuery query) {
        WorkflowTaskPageQuery resolved = query == null ? new WorkflowTaskPageQuery() : query;
        long offset = (resolved.getPage() - 1) * resolved.getSize();
        String initiator = currentUser();
        var instanceQuery = historyService.createHistoricProcessInstanceQuery()
                .variableValueEquals(INITIATOR_VAR, initiator)
                .orderByProcessInstanceStartTime()
                .desc();
        long total = instanceQuery.count();
        List<WorkflowProcessInstanceVO> records = instanceQuery
                .listPage(Math.toIntExact(offset), Math.toIntExact(resolved.getSize()))
                .stream()
                .map(this::fromHistoricInstance)
                .toList();
        return PageResult.of(records, total, resolved.getPage(), resolved.getSize());
    }

    @Override
    public WorkflowProcessDetailVO detail(String processInstanceId) {
        return workflowTaskRuntimeService.processDetail(processInstanceId);
    }

    @Override
    public PageResult<WorkflowProcessInstanceVO> historyByBusinessKey(String businessKey, WorkflowTaskPageQuery query) {
        WorkflowTaskPageQuery resolved = query == null ? new WorkflowTaskPageQuery() : query;
        long offset = (resolved.getPage() - 1) * resolved.getSize();
        var instanceQuery = historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .orderByProcessInstanceStartTime()
                .desc();
        long total = instanceQuery.count();
        List<WorkflowProcessInstanceVO> records = instanceQuery
                .listPage(Math.toIntExact(offset), Math.toIntExact(resolved.getSize()))
                .stream()
                .map(this::fromHistoricInstance)
                .toList();
        return PageResult.of(records, total, resolved.getPage(), resolved.getSize());
    }

    @Override
    public List<WorkflowBusinessProcessVO> latestByBusinessKeys(Collection<String> businessKeys) {
        return latestByBusinessKeys(null, businessKeys);
    }

    @Override
    public List<WorkflowBusinessProcessVO> latestByBusinessKeys(String businessType, Collection<String> businessKeys) {
        if (businessKeys == null || businessKeys.isEmpty()) {
            return List.of();
        }
        Map<String, io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO> applyProgress =
                workflowBusinessApplyService.latestProgress(businessType, businessKeys);
        if (!applyProgress.isEmpty()) {
            return applyProgress.values().stream()
                    .map(this::fromApplyProgress)
                    .toList();
        }
        if (StringUtils.hasText(businessType)) {
            return List.of();
        }
        return businessKeys.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .map(this::latestByBusinessKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private WorkflowBusinessProcessVO fromApplyProgress(io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO progress) {
        WorkflowBusinessProcessVO vo = new WorkflowBusinessProcessVO();
        vo.setBusinessKey(progress.getBusinessKey());
        vo.setBusinessType(progress.getBusinessType());
        vo.setApplyId(progress.getApplyId());
        vo.setApplyCode(progress.getApplyCode());
        vo.setProcessInstanceId(progress.getProcessInstanceId());
        vo.setProcessName(progress.getProcessName());
        vo.setCurrentTaskName(progress.getCurrentTaskNames());
        vo.setCurrentTaskDefinitionKey(progress.getCurrentTaskDefinitionKeys());
        vo.setStatus(progress.getApplyStatusName());
        vo.setApplyStatus(progress.getApplyStatus() == null ? null : progress.getApplyStatus().name());
        vo.setApplyStatusName(progress.getApplyStatusName());
        vo.setStartTime(progress.getCreatedAt());
        vo.setEndTime(progress.getUpdatedAt());
        return vo;
    }

    private WorkflowBusinessProcessVO latestByBusinessKey(String businessKey) {
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .orderByProcessInstanceStartTime()
                .desc()
                .listPage(0, 1)
                .stream()
                .findFirst()
                .orElse(null);
        if (instance == null) {
            return null;
        }
        WorkflowProcessInstanceVO process = fromHistoricInstance(instance);
        WorkflowBusinessProcessVO vo = new WorkflowBusinessProcessVO();
        vo.setBusinessKey(process.getBusinessKey());
        vo.setProcessInstanceId(process.getProcessInstanceId());
        vo.setProcessName(process.getProcessName());
        vo.setProcessKey(process.getProcessKey());
        vo.setCurrentTaskName(process.getCurrentTaskName());
        vo.setCurrentTaskDefinitionKey(process.getCurrentTaskDefinitionKey());
        vo.setStatus(process.getStatus());
        vo.setStartTime(process.getStartTime());
        vo.setEndTime(process.getEndTime());
        return vo;
    }

    private Long resolveApplyId(StartWorkflowProcessCommand command, WorkflowDefinitionEntity definition,
                                String businessKey, Map<String, Object> variables) {
        if (command.getApplyId() != null) {
            return command.getApplyId();
        }
        if (!StringUtils.hasText(command.getBusinessType())) {
            return null;
        }
        CreateWorkflowBusinessApplyCommand applyCommand = new CreateWorkflowBusinessApplyCommand();
        applyCommand.setBusinessType(command.getBusinessType().trim());
        applyCommand.setBusinessKey(businessKey);
        applyCommand.setApplyTitle(definition.getDefinitionName());
        applyCommand.setApplySummary(definition.getRemark());
        applyCommand.setProcessDefinitionId(definition.getId());
        applyCommand.setProcessDefinitionKey(definition.getDefinitionKey());
        applyCommand.setRenderMode(command.getRenderMode() == null
                ? resolveRenderMode(definition)
                : command.getRenderMode());
        applyCommand.setApplyPageKey(trim(command.getApplyPageKey()));
        applyCommand.setApprovePageKey(trim(command.getApprovePageKey()));
        applyCommand.setFormKey(definition.getFormCode());
        applyCommand.setFormVersion(definition.getPublishedVersionNo());
        applyCommand.setFormJsonSnapshot(definition.getFormJson());
        applyCommand.setSnapshotRef(trim(command.getSnapshotRef()));
        applyCommand.setVariables(WorkflowJsonRequest.of(variables));
        WorkflowBusinessApplyVO apply = workflowBusinessApplyService.create(applyCommand);
        if (apply == null || apply.getId() == null) {
            return null;
        }
        variables.put(APPLY_ID_VAR, String.valueOf(apply.getId()));
        applyCommand.setVariables(WorkflowJsonRequest.of(variables));
        return apply.getId();
    }

    private WorkflowProcessInstanceVO fromHistoricInstance(HistoricProcessInstance instance) {
        WorkflowProcessInstanceVO vo = new WorkflowProcessInstanceVO();
        vo.setProcessInstanceId(instance.getId());
        vo.setBusinessKey(instance.getBusinessKey());
        vo.setProcessName(instance.getProcessDefinitionName());
        vo.setProcessKey(instance.getProcessDefinitionKey());
        vo.setProcessDefinitionId(instance.getProcessDefinitionId());
        WorkflowFormInstanceEntity formInstance = formInstanceMapper.selectOne(new LambdaQueryWrapper<WorkflowFormInstanceEntity>()
                .eq(WorkflowFormInstanceEntity::getProcessInstanceId, instance.getId())
                .last("limit 1"));
        if (formInstance != null) {
            vo.setDefinitionId(formInstance.getDefinitionId());
            vo.setProcessName(formInstance.getDefinitionName());
            vo.setProcessKey(formInstance.getDefinitionKey());
            vo.setStatus(statusLabel(formInstance.getStatus(), instance.getEndTime() == null));
        } else {
            vo.setStatus(instance.getEndTime() == null
                    ? WorkflowInstanceStatus.RUNNING.getLabel()
                    : WorkflowInstanceStatus.COMPLETED.getLabel());
        }
        if (instance.getStartTime() != null) {
            vo.setStartTime(instance.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        if (instance.getEndTime() != null) {
            vo.setEndTime(instance.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        fillCurrentTask(instance.getId(), vo);
        return vo;
    }

    private void fillCurrentTask(String processInstanceId, WorkflowProcessInstanceVO vo) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .desc()
                .listPage(0, 1)
                .stream()
                .findFirst()
                .orElse(null);
        if (task == null) {
            return;
        }
        vo.setCurrentTaskName(task.getName());
        vo.setCurrentTaskDefinitionKey(task.getTaskDefinitionKey());
    }

    private void saveFormInstance(WorkflowDefinitionEntity definition, ProcessInstance instance, Map<String, Object> variables) {
        LocalDateTime now = LocalDateTime.now();
        WorkflowFormInstanceEntity formInstance = new WorkflowFormInstanceEntity();
        formInstance.setTenantId(currentTenantId());
        formInstance.setProcessInstanceId(instance.getProcessInstanceId());
        formInstance.setBusinessKey(instance.getBusinessKey());
        formInstance.setDefinitionId(definition.getId());
        formInstance.setDefinitionKey(definition.getDefinitionKey());
        formInstance.setDefinitionName(definition.getDefinitionName());
        formInstance.setProcessDefinitionId(definition.getProcessDefinitionId());
        formInstance.setProcessDefinitionVersion(definition.getProcessDefinitionVersion());
        formInstance.setFormCode(definition.getFormCode());
        formInstance.setFormJson(definition.getFormJson());
        formInstance.setVariablesJson(toJson(variables));
        formInstance.setStatus(WorkflowInstanceStatus.RUNNING.name());
        formInstance.setCreatedBy(MangoContextHolder.userId());
        formInstance.setCreatedTime(now);
        formInstance.setCreatedAt(now);
        formInstance.setUpdatedBy(MangoContextHolder.userId());
        formInstance.setUpdatedTime(now);
        formInstance.setUpdatedAt(now);
        formInstanceMapper.insert(formInstance);
    }

    private void saveStartRecord(String processInstanceId, Map<String, Object> variables) {
        LocalDateTime now = LocalDateTime.now();
        WorkflowTaskRecordEntity record = new WorkflowTaskRecordEntity();
        record.setTenantId(currentTenantId());
        record.setProcessInstanceId(processInstanceId);
        record.setAction(WorkflowTaskAction.START.name());
        record.setActionName(WorkflowTaskAction.START.getLabel());
        record.setOperatorId(MangoContextHolder.userId());
        record.setOperatorName(currentUser());
        record.setVariablesJson(toJson(variables));
        record.setCreatedTime(now);
        record.setCreatedAt(now);
        taskRecordMapper.insert(record);
    }

    private String statusLabel(String status, boolean running) {
        return WorkflowInstanceStatus.labelOf(status,
                running ? WorkflowInstanceStatus.RUNNING : WorkflowInstanceStatus.COMPLETED);
    }

    private WorkflowApplyRenderMode resolveRenderMode(WorkflowDefinitionEntity definition) {
        Map<String, Object> formConfig = parseMap(definition == null ? null : definition.getFormJson());
        String mode = formConfig == null ? null : trim(String.valueOf(formConfig.getOrDefault("mode", "")));
        return "CUSTOM".equalsIgnoreCase(mode) || "CUSTOM_PAGE".equalsIgnoreCase(mode)
                ? WorkflowApplyRenderMode.CUSTOM_PAGE
                : WorkflowApplyRenderMode.DYNAMIC_FORM;
    }

    private Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            Object value = objectMapper.readValue(json, Object.class);
            if (!(value instanceof Map<?, ?> raw)) {
                return Map.of();
            }
            Map<String, Object> result = new HashMap<>();
            raw.forEach((key, item) -> {
                if (key != null) {
                    result.put(String.valueOf(key), item);
                }
            });
            return result;
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private List<String> parseAdminUsers(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return cleanList(objectMapper.readValue(value, STRING_LIST_TYPE));
        } catch (JsonProcessingException e) {
            return cleanList(List.of(value.split("\\s*,\\s*")));
        }
    }

    private List<String> cleanList(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                set.add(value.trim());
            }
        }
        return new ArrayList<>(set);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String currentUser() {
        if (StringUtils.hasText(MangoContextHolder.principalName())) {
            return MangoContextHolder.principalName();
        }
        Long userId = MangoContextHolder.userId();
        return userId == null ? "anonymous" : String.valueOf(userId);
    }

    private Long currentTenantId() {
        if (!StringUtils.hasText(MangoContextHolder.tenantId())) {
            return 1L;
        }
        try {
            return Long.valueOf(MangoContextHolder.tenantId());
        } catch (NumberFormatException e) {
            return 1L;
        }
    }
}
