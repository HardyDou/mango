package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.workflow.api.WorkflowCode;
import io.mango.workflow.api.command.CompleteWorkflowTaskCommand;
import io.mango.workflow.api.command.RejectWorkflowTaskCommand;
import io.mango.workflow.api.enums.WorkflowEmptyAssigneeStrategy;
import io.mango.workflow.api.enums.WorkflowFormPermission;
import io.mango.workflow.api.enums.WorkflowInstanceStatus;
import io.mango.workflow.api.enums.WorkflowTaskAction;
import io.mango.workflow.api.enums.WorkflowTaskRuntimeStatus;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskRecordVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import io.mango.workflow.core.engine.WorkflowAssigneeResolver;
import io.mango.workflow.core.engine.WorkflowAssigneeCollection;
import io.mango.workflow.core.engine.WorkflowCandidateGroupProvider;
import io.mango.workflow.core.entity.WorkflowFormInstance;
import io.mango.workflow.core.entity.WorkflowTaskRecord;
import io.mango.workflow.core.event.WorkflowDomainEvents;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import io.mango.workflow.core.model.WorkflowApprovalNodeConfig;
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工作流任务运行时服务实现。
 */
@Service
@RequiredArgsConstructor
public class WorkflowTaskRuntimeServiceImpl implements IWorkflowTaskRuntimeService {

    private static final String DEFAULT_REJECT_REASON = "审批驳回";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final WorkflowFormInstanceMapper formInstanceMapper;
    private final WorkflowTaskRecordMapper taskRecordMapper;
    private final ObjectMapper objectMapper;
    private final WorkflowAssigneeResolver assigneeResolver;
    private final WorkflowCandidateGroupProvider candidateGroupProvider;
    private final ObjectProvider<IDomainEventPublisher> domainEventPublisherProvider;

    @Override
    public R<PageResult<WorkflowTaskVO>> todo(WorkflowTaskPageQuery query) {
        WorkflowTaskPageQuery resolved = resolve(query);
        long offset = (resolved.getPage() - 1) * resolved.getSize();
        var taskQuery = taskService.createTaskQuery();
        List<String> candidateGroups = candidateGroupProvider.currentCandidateGroups();
        if (isAdminUser()) {
            taskQuery.or().taskCandidateOrAssigned(currentUser());
            if (!candidateGroups.isEmpty()) {
                taskQuery.taskCandidateGroupIn(candidateGroups);
            }
            taskQuery.taskUnassigned().endOr();
        } else {
            taskQuery.or().taskCandidateOrAssigned(currentUser());
            if (!candidateGroups.isEmpty()) {
                taskQuery.taskCandidateGroupIn(candidateGroups);
            }
            taskQuery.endOr();
        }
        taskQuery.orderByTaskCreateTime().desc();
        if (StringUtils.hasText(resolved.getKeyword())) {
            taskQuery.taskNameLike("%" + resolved.getKeyword() + "%");
        }
        long total = taskQuery.count();
        List<WorkflowTaskVO> records = taskQuery
                .listPage(Math.toIntExact(offset), Math.toIntExact(resolved.getSize()))
                .stream()
                .map(this::fromTask)
                .toList();
        return R.ok(PageResult.of(records, total, resolved.getPage(), resolved.getSize()));
    }

    @Override
    public R<PageResult<WorkflowTaskVO>> done(WorkflowTaskPageQuery query) {
        WorkflowTaskPageQuery resolved = resolve(query);
        long offset = (resolved.getPage() - 1) * resolved.getSize();
        var taskQuery = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(currentUser())
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc();
        if (StringUtils.hasText(resolved.getKeyword())) {
            taskQuery.taskNameLike("%" + resolved.getKeyword() + "%");
        }
        long total = taskQuery.count();
        List<WorkflowTaskVO> records = taskQuery
                .listPage(Math.toIntExact(offset), Math.toIntExact(resolved.getSize()))
                .stream()
                .map(this::fromHistoricTask)
                .toList();
        return R.ok(PageResult.of(records, total, resolved.getPage(), resolved.getSize()));
    }

    @Override
    public R<WorkflowTaskDetailVO> detail(String taskId) {
        Require.notBlank(taskId, WorkflowCode.TASK_INVALID.getCode(), "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        WorkflowTaskDetailVO vo = new WorkflowTaskDetailVO();
        vo.setTask(fromTask(task));
        vo.setProcess(processInfo(task.getProcessInstanceId()));
        WorkflowFormInstance formInstance = findFormInstance(task.getProcessInstanceId());
        fillForm(vo, formInstance, readRuntimeVariables(task.getProcessInstanceId()));
        vo.setFormPermissions(taskFormPermissions(task));
        vo.setRecords(records(task.getProcessInstanceId()));
        return R.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> complete(CompleteWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID.getCode(), "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        ensureCurrentUserCanOperate(task);

        Map<String, Object> variables = mergeVariables(task.getProcessInstanceId(), command.getVariables());
        if (StringUtils.hasText(command.getComment())) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), command.getComment().trim());
        }
        claimIfUnassigned(task);
        taskService.complete(task.getId(), variables);
        saveRecord(task, WorkflowTaskAction.COMPLETE, command.getComment(), command.getVariables());
        boolean ended = isProcessEnded(task.getProcessInstanceId());
        updateFormInstance(task.getProcessInstanceId(), variables,
                ended ? WorkflowInstanceStatus.COMPLETED : WorkflowInstanceStatus.RUNNING);
        publishTaskEvent(WorkflowDomainEvents.TASK_COMPLETED, task, variables, command.getComment());
        if (ended) {
            publishProcessEvent(WorkflowDomainEvents.PROCESS_COMPLETED, task.getProcessInstanceId(), variables);
        }
        triggerEventNotify(task, variables);
        advanceRuntimeTasks(task.getProcessInstanceId());
        return R.ok(Boolean.TRUE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> reject(RejectWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID.getCode(), "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        ensureCurrentUserCanOperate(task);

        Map<String, Object> variables = mergeVariables(task.getProcessInstanceId(), command.getVariables());
        if (StringUtils.hasText(command.getComment())) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), command.getComment().trim());
        }
        claimIfUnassigned(task);
        saveRecord(task, WorkflowTaskAction.REJECT, command.getComment(), command.getVariables());
        triggerEventNotify(task, variables);
        runtimeService.deleteProcessInstance(task.getProcessInstanceId(),
                StringUtils.hasText(command.getComment()) ? command.getComment().trim() : DEFAULT_REJECT_REASON);
        updateFormInstance(task.getProcessInstanceId(), variables, WorkflowInstanceStatus.REJECTED);
        publishTaskEvent(WorkflowDomainEvents.TASK_REJECTED, task, variables, command.getComment());
        publishProcessEvent(WorkflowDomainEvents.PROCESS_ENDED, task.getProcessInstanceId(), variables);
        return R.ok(Boolean.TRUE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advanceRuntimeTasks(String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId) || isProcessEnded(processInstanceId)) {
            return;
        }
        for (int i = 0; i < 16; i++) {
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
            boolean changed = false;
            for (Task task : tasks) {
                changed = resolveRuntimeTask(task) || changed;
            }
            updateFormInstance(processInstanceId, readStoredVariables(processInstanceId),
                    isProcessEnded(processInstanceId) ? WorkflowInstanceStatus.COMPLETED : WorkflowInstanceStatus.RUNNING);
            if (!changed || isProcessEnded(processInstanceId)) {
                return;
            }
        }
    }

    @Override
    public R<WorkflowProcessDetailVO> processDetail(String processInstanceId) {
        Require.notBlank(processInstanceId, WorkflowCode.PROCESS_INSTANCE_NOT_FOUND.getCode(), "流程实例ID不能为空");
        WorkflowProcessDetailVO vo = new WorkflowProcessDetailVO();
        vo.setProcess(processInfo(processInstanceId));
        WorkflowFormInstance formInstance = findFormInstance(processInstanceId);
        vo.setFormCode(formInstance == null ? null : formInstance.getFormCode());
        vo.setFormJson(formInstance == null ? null : formInstance.getFormJson());
        vo.setVariables(formInstance == null ? readRuntimeVariables(processInstanceId) : parseMap(formInstance.getVariablesJson()));
        vo.setRecords(records(processInstanceId));
        return R.ok(vo);
    }

    private WorkflowTaskPageQuery resolve(WorkflowTaskPageQuery query) {
        return query == null ? new WorkflowTaskPageQuery() : query;
    }

    private void fillForm(WorkflowTaskDetailVO vo, WorkflowFormInstance formInstance, Map<String, Object> runtimeVariables) {
        vo.setFormCode(formInstance == null ? null : formInstance.getFormCode());
        vo.setFormJson(formInstance == null ? null : formInstance.getFormJson());
        vo.setVariables(formInstance == null ? runtimeVariables : parseMap(formInstance.getVariablesJson()));
    }

    private WorkflowProcessInstanceVO processInfo(String processInstanceId) {
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        Require.notNull(instance, WorkflowCode.PROCESS_INSTANCE_NOT_FOUND);
        WorkflowProcessInstanceVO vo = new WorkflowProcessInstanceVO();
        vo.setProcessInstanceId(instance.getId());
        vo.setBusinessKey(instance.getBusinessKey());
        vo.setProcessName(instance.getProcessDefinitionName());
        vo.setProcessKey(instance.getProcessDefinitionKey());
        vo.setProcessDefinitionId(instance.getProcessDefinitionId());
        vo.setStatus(resolveStatus(processInstanceId, instance));
        if (instance.getStartTime() != null) {
            vo.setStartTime(instance.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        if (instance.getEndTime() != null) {
            vo.setEndTime(instance.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        WorkflowFormInstance formInstance = findFormInstance(processInstanceId);
        if (formInstance != null) {
            vo.setDefinitionId(formInstance.getDefinitionId());
            vo.setProcessName(formInstance.getDefinitionName());
            vo.setProcessKey(formInstance.getDefinitionKey());
        }
        HistoricVariableInstance initiatorVariable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName("mangoInitiatorName")
                .singleResult();
        Object initiator = initiatorVariable == null ? null : initiatorVariable.getValue();
        vo.setInitiatorName(initiator == null ? null : String.valueOf(initiator));
        return vo;
    }

    private String resolveStatus(String processInstanceId, HistoricProcessInstance instance) {
        WorkflowFormInstance formInstance = findFormInstance(processInstanceId);
        if (formInstance != null && StringUtils.hasText(formInstance.getStatus())) {
            return WorkflowInstanceStatus.labelOf(formInstance.getStatus(), WorkflowInstanceStatus.RUNNING);
        }
        return instance.getEndTime() == null ? WorkflowInstanceStatus.RUNNING.getLabel() : WorkflowInstanceStatus.COMPLETED.getLabel();
    }

    private WorkflowTaskVO fromTask(Task task) {
        WorkflowTaskVO vo = new WorkflowTaskVO();
        vo.setId(task.getId());
        vo.setTaskName(task.getName());
        vo.setTaskDefinitionKey(task.getTaskDefinitionKey());
        vo.setProcessInstanceId(task.getProcessInstanceId());
        vo.setProcessDefinitionId(task.getProcessDefinitionId());
        vo.setAssigneeName(task.getAssignee());
        vo.setStatus(WorkflowTaskRuntimeStatus.TODO.getLabel());
        if (task.getCreateTime() != null) {
            vo.setCreateTime(task.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        WorkflowFormInstance formInstance = findFormInstance(task.getProcessInstanceId());
        if (formInstance != null) {
            vo.setBusinessKey(formInstance.getBusinessKey());
            vo.setProcessName(formInstance.getDefinitionName());
            vo.setProcessKey(formInstance.getDefinitionKey());
        } else {
            vo.setProcessName(task.getProcessDefinitionId());
            vo.setProcessKey(task.getProcessDefinitionId());
        }
        return vo;
    }

    private WorkflowTaskVO fromHistoricTask(HistoricTaskInstance task) {
        WorkflowTaskVO vo = new WorkflowTaskVO();
        vo.setId(task.getId());
        vo.setTaskName(task.getName());
        vo.setTaskDefinitionKey(task.getTaskDefinitionKey());
        vo.setProcessInstanceId(task.getProcessInstanceId());
        vo.setProcessDefinitionId(task.getProcessDefinitionId());
        vo.setAssigneeName(task.getAssignee());
        vo.setStatus(WorkflowTaskRuntimeStatus.DONE.getLabel());
        if (task.getCreateTime() != null) {
            vo.setCreateTime(task.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        if (task.getEndTime() != null) {
            vo.setEndTime(task.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        WorkflowFormInstance formInstance = findFormInstance(task.getProcessInstanceId());
        if (formInstance != null) {
            vo.setBusinessKey(formInstance.getBusinessKey());
            vo.setProcessName(formInstance.getDefinitionName());
            vo.setProcessKey(formInstance.getDefinitionKey());
        } else {
            vo.setProcessName(task.getProcessDefinitionId());
            vo.setProcessKey(task.getProcessDefinitionId());
        }
        return vo;
    }

    private List<WorkflowTaskRecordVO> records(String processInstanceId) {
        return taskRecordMapper.selectList(new LambdaQueryWrapper<WorkflowTaskRecord>()
                        .eq(WorkflowTaskRecord::getProcessInstanceId, processInstanceId)
                        .orderByAsc(WorkflowTaskRecord::getCreatedTime)
                        .orderByAsc(WorkflowTaskRecord::getId))
                .stream()
                .map(this::fromRecord)
                .toList();
    }

    private WorkflowTaskRecordVO fromRecord(WorkflowTaskRecord record) {
        WorkflowTaskRecordVO vo = new WorkflowTaskRecordVO();
        vo.setId(record.getId());
        vo.setProcessInstanceId(record.getProcessInstanceId());
        vo.setTaskId(record.getTaskId());
        vo.setTaskName(record.getTaskName());
        vo.setTaskDefinitionKey(record.getTaskDefinitionKey());
        vo.setAction(record.getAction());
        vo.setActionName(StringUtils.hasText(record.getActionName())
                ? record.getActionName()
                : WorkflowTaskAction.labelOf(record.getAction()));
        vo.setOperatorId(record.getOperatorId());
        vo.setOperatorName(record.getOperatorName());
        vo.setComment(record.getComment());
        vo.setVariables(parseMap(record.getVariablesJson()));
        vo.setCreatedTime(record.getCreatedTime());
        return vo;
    }

    private WorkflowFormInstance findFormInstance(String processInstanceId) {
        return formInstanceMapper.selectOne(new LambdaQueryWrapper<WorkflowFormInstance>()
                .eq(WorkflowFormInstance::getProcessInstanceId, processInstanceId)
                .last("limit 1"));
    }

    private Map<String, Object> mergeVariables(String processInstanceId, Map<String, Object> submitted) {
        Map<String, Object> variables = new LinkedHashMap<>(readStoredVariables(processInstanceId));
        if (submitted != null) {
            variables.putAll(submitted);
        }
        return variables;
    }

    private Map<String, Object> readStoredVariables(String processInstanceId) {
        WorkflowFormInstance formInstance = findFormInstance(processInstanceId);
        if (formInstance != null) {
            return parseMap(formInstance.getVariablesJson());
        }
        return readRuntimeVariables(processInstanceId);
    }

    private Map<String, Object> readRuntimeVariables(String processInstanceId) {
        if (runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult() == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(runtimeService.getVariables(processInstanceId));
    }

    private void saveRecord(Task task, WorkflowTaskAction action, String comment, Map<String, Object> variables) {
        WorkflowTaskRecord record = new WorkflowTaskRecord();
        record.setTenantId(currentTenantId());
        record.setProcessInstanceId(task.getProcessInstanceId());
        record.setTaskId(task.getId());
        record.setTaskName(task.getName());
        record.setTaskDefinitionKey(task.getTaskDefinitionKey());
        record.setAction(action.name());
        record.setActionName(action.getLabel());
        record.setOperatorId(MangoContextHolder.userId());
        record.setOperatorName(currentUser());
        record.setComment(StringUtils.hasText(comment) ? comment.trim() : null);
        record.setVariablesJson(toJson(variables == null ? Map.of() : variables));
        LocalDateTime now = LocalDateTime.now();
        record.setCreatedTime(now);
        record.setCreatedAt(now);
        taskRecordMapper.insert(record);
    }

    private void updateFormInstance(String processInstanceId, Map<String, Object> variables, WorkflowInstanceStatus status) {
        LocalDateTime now = LocalDateTime.now();
        formInstanceMapper.update(null, new LambdaUpdateWrapper<WorkflowFormInstance>()
                .eq(WorkflowFormInstance::getProcessInstanceId, processInstanceId)
                .set(WorkflowFormInstance::getVariablesJson, toJson(variables))
                .set(WorkflowFormInstance::getStatus, status.name())
                .set(WorkflowFormInstance::getUpdatedBy, MangoContextHolder.userId())
                .set(WorkflowFormInstance::getUpdatedTime, now)
                .set(WorkflowFormInstance::getUpdatedAt, now));
    }

    private boolean isProcessEnded(String processInstanceId) {
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        return instance != null && instance.getEndTime() != null;
    }

    private void ensureCurrentUserCanOperate(Task task) {
        String currentUser = currentUser();
        List<String> candidateGroups = candidateGroupProvider.currentCandidateGroups();
        boolean assigned = currentUser.equals(task.getAssignee());
        boolean unassignedAdmin = !StringUtils.hasText(task.getAssignee()) && isAdminUser();
        boolean candidate = taskService.createTaskQuery()
                .taskId(task.getId())
                .taskCandidateOrAssigned(currentUser)
                .count() > 0;
        boolean groupCandidate = !candidateGroups.isEmpty() && taskService.createTaskQuery()
                .taskId(task.getId())
                .taskCandidateGroupIn(candidateGroups)
                .count() > 0;
        Require.isTrue(assigned || candidate || groupCandidate || unassignedAdmin, WorkflowCode.TASK_INVALID.getCode(), "当前用户不能处理该任务");
    }

    private void claimIfUnassigned(Task task) {
        if (!StringUtils.hasText(task.getAssignee())) {
            taskService.setAssignee(task.getId(), currentUser());
            task.setAssignee(currentUser());
        }
    }

    private boolean resolveRuntimeTask(Task task) {
        WorkflowApprovalNodeConfig config = taskApprovalConfig(task);
        if (config == null) {
            return false;
        }
        if (WorkflowAssigneeCollection.EMPTY_ASSIGNEE.equals(task.getAssignee())) {
            WorkflowEmptyAssigneeStrategy strategy = config.getEmptyAssigneeStrategy() == null
                    ? WorkflowEmptyAssigneeStrategy.TO_ADMIN
                    : config.getEmptyAssigneeStrategy();
            if (strategy == WorkflowEmptyAssigneeStrategy.TO_ADMIN) {
                taskService.setAssignee(task.getId(), definitionAdminUsers(task.getProcessInstanceId()).stream()
                        .findFirst()
                        .orElse(WorkflowAssigneeResolver.ADMIN_USER));
                return true;
            }
            if (strategy == WorkflowEmptyAssigneeStrategy.TO_USER
                    && config.getEmptyAssigneeUserIds() != null
                    && !config.getEmptyAssigneeUserIds().isEmpty()) {
                taskService.setAssignee(task.getId(), config.getEmptyAssigneeUserIds().get(0));
                return true;
            }
            return applyAutoEmptyStrategy(task, strategy, readStoredVariables(task.getProcessInstanceId()));
        }
        boolean changed = false;
        Map<String, Object> variables = readStoredVariables(task.getProcessInstanceId());
        WorkflowAssigneeResolver.ResolvedAssignees resolved = assigneeResolver.applyEmptyStrategy(config,
                assigneeResolver.resolve(config, variables, initiator(task.getProcessInstanceId()), task.getTaskDefinitionKey()),
                variables);
        if (resolved.empty()) {
            return applyAutoEmptyStrategy(task, resolved.emptyStrategy(), variables);
        }
        if (resolved.users() != null && !resolved.users().isEmpty()) {
            if (task.getAssignee() != null && task.getAssignee().startsWith("${mangoRuntimeAssignee_")) {
                taskService.setAssignee(task.getId(), resolved.users().get(0));
                changed = true;
            }
            String multiVariable = "mangoAssignees_" + task.getTaskDefinitionKey();
            if (!variables.containsKey(multiVariable)) {
                variables.put(multiVariable, resolved.users());
                runtimeService.setVariable(task.getProcessInstanceId(), multiVariable, resolved.users());
                changed = true;
            }
        }
        if (resolved.groups() != null && !resolved.groups().isEmpty() && taskIdentityGroups(task.getId()).isEmpty()) {
            for (String group : resolved.groups()) {
                taskService.addCandidateGroup(task.getId(), group);
            }
            changed = true;
        }
        return changed;
    }

    private boolean applyAutoEmptyStrategy(Task task, WorkflowEmptyAssigneeStrategy strategy, Map<String, Object> variables) {
        if (strategy == WorkflowEmptyAssigneeStrategy.AUTO_PASS) {
            taskService.complete(task.getId(), variables);
            saveRecord(task, WorkflowTaskAction.AUTO_COMPLETE, "审批人为空，系统自动通过", variables);
            return true;
        }
        if (strategy == WorkflowEmptyAssigneeStrategy.AUTO_REJECT) {
            saveRecord(task, WorkflowTaskAction.AUTO_REJECT, "审批人为空，系统自动驳回", variables);
            runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "审批人为空，系统自动驳回");
            updateFormInstance(task.getProcessInstanceId(), variables, WorkflowInstanceStatus.REJECTED);
            publishTaskEvent(WorkflowDomainEvents.TASK_REJECTED, task, variables, "审批人为空，系统自动驳回");
            publishProcessEvent(WorkflowDomainEvents.PROCESS_ENDED, task.getProcessInstanceId(), variables);
            return true;
        }
        if (strategy == WorkflowEmptyAssigneeStrategy.AUTO_END) {
            saveRecord(task, WorkflowTaskAction.AUTO_END, "审批人为空，系统自动结束", variables);
            runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "审批人为空，系统自动结束");
            updateFormInstance(task.getProcessInstanceId(), variables, WorkflowInstanceStatus.ENDED);
            publishProcessEvent(WorkflowDomainEvents.PROCESS_ENDED, task.getProcessInstanceId(), variables);
            return true;
        }
        return false;
    }

    private void publishTaskEvent(String eventType, Task task, Map<String, Object> variables, String comment) {
        IDomainEventPublisher publisher = domainEventPublisherProvider.getIfAvailable();
        if (publisher == null) {
            return;
        }
        publisher.publish(baseEvent(eventType, task.getProcessInstanceId(), variables)
                .payload("taskId", task.getId())
                .payload("taskName", task.getName())
                .payload("taskDefinitionKey", task.getTaskDefinitionKey())
                .payload("assignee", task.getAssignee())
                .payload("comment", comment)
                .payload("variables", variables)
                .build());
    }

    private void publishProcessEvent(String eventType, String processInstanceId, Map<String, Object> variables) {
        IDomainEventPublisher publisher = domainEventPublisherProvider.getIfAvailable();
        if (publisher == null) {
            return;
        }
        publisher.publish(baseEvent(eventType, processInstanceId, variables)
                .payload("processInstanceId", processInstanceId)
                .payload("variables", variables)
                .build());
    }

    private DomainEvent.DomainEventBuilder baseEvent(String eventType, String processInstanceId, Map<String, Object> variables) {
        WorkflowFormInstance formInstance = findFormInstance(processInstanceId);
        return DomainEvent.builder()
                .eventType(eventType)
                .businessType(stringVar(variables, "businessType"))
                .businessKey(formInstance == null ? stringVar(variables, "businessKey") : formInstance.getBusinessKey())
                .aggregateId(stringVar(variables, "applyId"))
                .payload("processInstanceId", processInstanceId);
    }

    private List<String> definitionAdminUsers(String processInstanceId) {
        Object value = readStoredVariables(processInstanceId).get(WorkflowAssigneeResolver.DEFINITION_ADMIN_USERS_VAR);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(StringUtils::hasText).toList();
        }
        return Arrays.stream(String.valueOf(value).split("\\s*,\\s*"))
                .filter(StringUtils::hasText)
                .toList();
    }

    private Set<String> taskIdentityGroups(String taskId) {
        Set<String> groups = new LinkedHashSet<>();
        for (IdentityLink link : taskService.getIdentityLinksForTask(taskId)) {
            if (StringUtils.hasText(link.getGroupId())) {
                groups.add(link.getGroupId());
            }
        }
        return groups;
    }

    private Map<String, String> taskFormPermissions(Task task) {
        WorkflowApprovalNodeConfig config = taskApprovalConfig(task);
        Map<String, String> permissions = new LinkedHashMap<>();
        if (config == null || config.getFormPermissions() == null || config.getFormPermissions().isEmpty()) {
            return permissions;
        }
        config.getFormPermissions().forEach((field, permission) ->
                permissions.put(field, (permission == null ? WorkflowFormPermission.READONLY : permission).name()));
        return permissions;
    }

    private WorkflowApprovalNodeConfig taskApprovalConfig(Task task) {
        if (task == null || !StringUtils.hasText(task.getProcessDefinitionId()) || !StringUtils.hasText(task.getTaskDefinitionKey())) {
            return null;
        }
        FlowElement element = repositoryService.getBpmnModel(task.getProcessDefinitionId()).getFlowElement(task.getTaskDefinitionKey());
        if (!(element instanceof UserTask userTask)) {
            return null;
        }
        List<ExtensionElement> elements = userTask.getExtensionElements().get("mangoApprovalConfig");
        if (elements == null || elements.isEmpty() || !StringUtils.hasText(elements.get(0).getElementText())) {
            return null;
        }
        try {
            return objectMapper.readValue(elements.get(0).getElementText(), WorkflowApprovalNodeConfig.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String initiator(String processInstanceId) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName("mangoInitiator")
                .singleResult();
        Object value = variable == null ? readStoredVariables(processInstanceId).get("mangoInitiator") : variable.getValue();
        return value == null ? currentUser() : String.valueOf(value);
    }

    private void triggerEventNotify(Task task, Map<String, Object> variables) {
        WorkflowApprovalNodeConfig config = taskApprovalConfig(task);
        if (config == null || !config.hasEventNotify()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getId());
        payload.put("taskName", task.getName());
        payload.put("taskDefinitionKey", task.getTaskDefinitionKey());
        payload.put("processInstanceId", task.getProcessInstanceId());
        payload.put("eventNotify", config.getEventNotify());
        payload.put("variables", variables);
        saveRecord(task, WorkflowTaskAction.EVENT_NOTIFY, "节点事件通知已记录", payload);
    }

    private Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, MAP_TYPE);
            return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String stringVar(Map<String, Object> variables, String key) {
        Object value = variables == null ? null : variables.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String currentUser() {
        if (StringUtils.hasText(MangoContextHolder.principalName())) {
            return MangoContextHolder.principalName();
        }
        Long userId = MangoContextHolder.userId();
        return userId == null ? "anonymous" : String.valueOf(userId);
    }

    private boolean isAdminUser() {
        return "admin".equals(currentUser());
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
