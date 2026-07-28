package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.command.AddSignWorkflowTaskCommand;
import io.mango.workflow.api.command.ClaimWorkflowTaskCommand;
import io.mango.workflow.api.command.CompleteWorkflowTaskCommand;
import io.mango.workflow.api.command.ReadWorkflowCopiedTaskCommand;
import io.mango.workflow.api.command.RejectWorkflowTaskCommand;
import io.mango.workflow.api.command.ReturnWorkflowTaskCommand;
import io.mango.workflow.api.command.SaveWorkflowTaskDraftCommand;
import io.mango.workflow.api.command.TransferWorkflowTaskCommand;
import io.mango.workflow.api.command.WorkflowJsonRequest;
import io.mango.workflow.api.enums.WorkflowEmptyAssigneeStrategy;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.mango.workflow.api.enums.WorkflowFormPermission;
import io.mango.workflow.api.enums.WorkflowInstanceStatus;
import io.mango.workflow.api.enums.WorkflowTaskAction;
import io.mango.workflow.api.enums.WorkflowTaskClaimStatus;
import io.mango.workflow.api.enums.WorkflowTaskRuntimeStatus;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessApplyCurrentTaskVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.api.vo.WorkflowJsonVO;
import io.mango.workflow.api.vo.WorkflowMyTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowNodeActionConfigVO;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import io.mango.workflow.api.vo.WorkflowRenderConfigVO;
import io.mango.workflow.api.vo.WorkflowTaskActionResultVO;
import io.mango.workflow.api.vo.WorkflowTaskCompleteResultVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskRecordVO;
import io.mango.workflow.api.vo.WorkflowTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import io.mango.workflow.core.engine.WorkflowAssigneeResolver;
import io.mango.workflow.core.engine.WorkflowAssigneeCollection;
import io.mango.workflow.core.engine.WorkflowCandidateGroupProvider;
import io.mango.workflow.core.engine.WorkflowNodeExecutionEvent;
import io.mango.workflow.core.entity.WorkflowBusinessApplyEntity;
import io.mango.workflow.core.entity.WorkflowCopiedTaskEntity;
import io.mango.workflow.core.entity.WorkflowDefinitionEntity;
import io.mango.workflow.core.entity.WorkflowDefinitionVersionEntity;
import io.mango.workflow.core.entity.WorkflowFormInstanceEntity;
import io.mango.workflow.core.entity.WorkflowTaskRecordEntity;
import io.mango.workflow.core.event.WorkflowEventPublisher;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import io.mango.workflow.core.mapper.WorkflowCopiedTaskMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionVersionMapper;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import io.mango.workflow.core.model.WorkflowApprovalNodeConfig;
import io.mango.workflow.core.model.WorkflowTaskStatusContext;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import io.mango.workflow.core.service.IWorkflowTaskRuntimeService;
import io.mango.workflow.core.service.WorkflowTaskAdvanceResult;
import io.mango.workflow.core.support.WorkflowNodeActionConfigResolver;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 工作流任务运行时服务实现。
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring singleton collaborators are injected dependencies, not owned mutable state"))
public class WorkflowTaskRuntimeService implements IWorkflowTaskRuntimeService {

    private static final String CLAIMED_FROM_CANDIDATE_VARIABLE = "mangoClaimedFromCandidate";

    private static final String DEFAULT_REJECT_REASON = "审批驳回";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final WorkflowBusinessApplyMapper businessApplyMapper;
    private final WorkflowCopiedTaskMapper copiedTaskMapper;
    private final WorkflowDefinitionMapper definitionMapper;
    private final WorkflowDefinitionVersionMapper definitionVersionMapper;
    private final WorkflowFormInstanceMapper formInstanceMapper;
    private final WorkflowTaskRecordMapper taskRecordMapper;
    private final ObjectMapper objectMapper;
    private final WorkflowAssigneeResolver assigneeResolver;
    private final WorkflowCandidateGroupProvider candidateGroupProvider;
    private final IWorkflowBusinessApplyService workflowBusinessApplyService;
    private final WorkflowEventPublisher workflowEventPublisher;

    @Override
    public PageResult<WorkflowTaskVO> todo(WorkflowTaskPageQuery query) {
        WorkflowTaskPageQuery resolved = resolve(query);
        long offset = (resolved.getPage() - 1) * resolved.getSize();
        var taskQuery = taskService.createTaskQuery();
        String keyword = trim(resolved.getKeyword());
        List<String> businessProcessInstanceIds = businessProcessInstanceIds(keyword);
        List<String> candidateGroups = candidateGroupProvider.currentCandidateGroups();
        applyTodoTypeFilter(taskQuery, resolved.getTodoType(), candidateGroups);
        if (Boolean.TRUE.equals(resolved.getOverdue())) {
            taskQuery.taskDueBefore(new Date());
        }
        taskQuery.orderByTaskCreateTime().desc();
        if (StringUtils.hasText(keyword)) {
            taskQuery.or()
                    .taskNameLike("%" + keyword + "%")
                    .processDefinitionNameLike("%" + keyword + "%")
                    .processInstanceBusinessKeyLike("%" + keyword + "%")
                    .processVariableValueLike("businessKey", "%" + keyword + "%");
            if (!businessProcessInstanceIds.isEmpty()) {
                taskQuery.processInstanceIdIn(businessProcessInstanceIds);
            }
            taskQuery.endOr();
        }
        long total = taskQuery.count();
        List<WorkflowTaskVO> records = taskQuery
                .listPage(Math.toIntExact(offset), Math.toIntExact(resolved.getSize()))
                .stream()
                .map(this::fromTask)
                .toList();
        return PageResult.of(records, total, resolved.getPage(), resolved.getSize());
    }

    @Override
    public PageResult<WorkflowTaskVO> initiated(WorkflowTaskPageQuery query) {
        WorkflowTaskPageQuery resolved = resolve(query);
        return PageResult.of(List.of(), 0, resolved.getPage(), resolved.getSize());
    }

    private void applyTodoTypeFilter(TaskQuery taskQuery, String todoType, List<String> candidateGroups) {
        String type = StringUtils.hasText(todoType) ? todoType.trim().toUpperCase() : "ASSIGNED";
        if ("CLAIMABLE".equals(type)) {
            taskQuery.or().taskCandidateUser(currentUser());
            if (!candidateGroups.isEmpty()) {
                taskQuery.taskCandidateGroupIn(candidateGroups);
            }
            if (isAdminUser()) {
                taskQuery.taskUnassigned();
            }
            taskQuery.endOr();
            return;
        }
        if ("ALL".equals(type)) {
            taskQuery.or().taskCandidateOrAssigned(currentUser());
            if (!candidateGroups.isEmpty()) {
                taskQuery.taskCandidateGroupIn(candidateGroups);
            }
            if (isAdminUser()) {
                taskQuery.taskUnassigned();
            }
            taskQuery.endOr();
            return;
        }
        taskQuery.taskAssignee(currentUser());
    }

    @Override
    public WorkflowTaskSummaryVO summary() {
        List<String> candidateGroups = candidateGroupProvider.currentCandidateGroups();
        WorkflowTaskSummaryVO vo = new WorkflowTaskSummaryVO();
        vo.setPendingApproval(countTodoByType("ASSIGNED", candidateGroups));
        vo.setPendingHandle(countTodoByType("CLAIMABLE", candidateGroups));
        vo.setPendingConfirm(countUnreadCopied());
        vo.setOverdue(countOverdueTasks(candidateGroups));
        return vo;
    }

    @Override
    public WorkflowMyTaskSummaryVO myTaskSummary() {
        List<String> candidateGroups = candidateGroupProvider.currentCandidateGroups();
        Long pending = countTodoByType("CLAIMABLE", candidateGroups);
        Long processing = countTodoByType("ASSIGNED", candidateGroups);
        Long completed = countCompletedTasks();
        Long overdue = countOverdueTasks(candidateGroups);
        WorkflowMyTaskSummaryVO vo = new WorkflowMyTaskSummaryVO();
        vo.setPending(pending);
        vo.setProcessing(processing);
        vo.setCompleted(completed);
        vo.setOverdue(overdue);
        vo.setTotal(pending + processing + completed + overdue);
        return vo;
    }

    private Long countTodoByType(String todoType, List<String> candidateGroups) {
        TaskQuery taskQuery = taskService.createTaskQuery();
        applyTodoTypeFilter(taskQuery, todoType, candidateGroups);
        return taskQuery.count();
    }

    private Long countCompletedTasks() {
        return historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(currentUser())
                .finished()
                .count();
    }

    private Long countUnreadCopied() {
        return copiedTaskMapper.selectCount(new LambdaQueryWrapper<WorkflowCopiedTaskEntity>()
                .eq(WorkflowCopiedTaskEntity::getCopiedUserId, currentUser())
                .eq(WorkflowCopiedTaskEntity::getReadFlag, Boolean.FALSE));
    }

    private Long countOverdueTasks(List<String> candidateGroups) {
        Date now = new Date();
        Set<String> taskIds = new LinkedHashSet<>();
        taskIds.addAll(overdueAssignedTasks(now).stream().map(Task::getId).toList());
        taskIds.addAll(overdueClaimableTasks(now, candidateGroups).stream().map(Task::getId).toList());
        return (long) taskIds.size();
    }

    private List<Task> overdueAssignedTasks(Date now) {
        return taskService.createTaskQuery()
                .taskAssignee(currentUser())
                .taskDueBefore(now)
                .list();
    }

    private List<Task> overdueClaimableTasks(Date now, List<String> candidateGroups) {
        TaskQuery taskQuery = taskService.createTaskQuery();
        applyTodoTypeFilter(taskQuery, "CLAIMABLE", candidateGroups);
        return taskQuery
                .taskDueBefore(now)
                .list();
    }

    @Override
    public PageResult<WorkflowTaskVO> done(WorkflowTaskPageQuery query) {
        WorkflowTaskPageQuery resolved = resolve(query);
        long offset = (resolved.getPage() - 1) * resolved.getSize();
        String keyword = trim(resolved.getKeyword());
        List<String> businessProcessInstanceIds = businessProcessInstanceIds(keyword);
        var taskQuery = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(currentUser())
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc();
        if (StringUtils.hasText(keyword)) {
            taskQuery.or()
                    .taskNameLike("%" + keyword + "%")
                    .processDefinitionNameLike("%" + keyword + "%")
                    .processInstanceBusinessKeyLike("%" + keyword + "%")
                    .processVariableValueLike("businessKey", "%" + keyword + "%");
            if (!businessProcessInstanceIds.isEmpty()) {
                taskQuery.processInstanceIdIn(businessProcessInstanceIds);
            }
            taskQuery.endOr();
        }
        long total = taskQuery.count();
        List<WorkflowTaskVO> records = taskQuery
                .listPage(Math.toIntExact(offset), Math.toIntExact(resolved.getSize()))
                .stream()
                .map(this::fromHistoricTask)
                .toList();
        return PageResult.of(records, total, resolved.getPage(), resolved.getSize());
    }

    @Override
    public PageResult<WorkflowTaskVO> copied(WorkflowTaskPageQuery query) {
        WorkflowTaskPageQuery resolved = resolve(query);
        long offset = (resolved.getPage() - 1) * resolved.getSize();
        String keyword = trim(resolved.getKeyword());
        LambdaQueryWrapper<WorkflowCopiedTaskEntity> wrapper = new LambdaQueryWrapper<WorkflowCopiedTaskEntity>()
                .eq(WorkflowCopiedTaskEntity::getCopiedUserId, currentUser());
        if (Boolean.TRUE.equals(resolved.getUnread())) {
            wrapper.eq(WorkflowCopiedTaskEntity::getReadFlag, Boolean.FALSE);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item
                    .like(WorkflowCopiedTaskEntity::getProcessName, keyword)
                    .or()
                    .like(WorkflowCopiedTaskEntity::getBusinessKey, keyword)
                    .or()
                    .like(WorkflowCopiedTaskEntity::getNodeName, keyword)
                    .or()
                    .like(WorkflowCopiedTaskEntity::getMessage, keyword));
        }
        long total = copiedTaskMapper.selectCount(wrapper);
        List<WorkflowTaskVO> records = copiedTaskMapper.selectList(wrapper
                        .orderByDesc(WorkflowCopiedTaskEntity::getCreatedTime)
                        .last("limit " + offset + "," + resolved.getSize()))
                .stream()
                .map(this::fromCopiedTask)
                .toList();
        return PageResult.of(records, total, resolved.getPage(), resolved.getSize());
    }

    @Override
    public WorkflowTaskDetailVO detail(String taskId) {
        Require.notBlank(taskId, WorkflowCode.TASK_INVALID, "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        WorkflowTaskDetailVO vo = new WorkflowTaskDetailVO();
        vo.setTask(fromTask(task));
        vo.setProcess(processInfo(task.getProcessInstanceId()));
        WorkflowFormInstanceEntity formInstance = findFormInstance(task.getProcessInstanceId());
        Map<String, Object> runtimeVariables = readRuntimeVariables(task.getProcessInstanceId());
        WorkflowDefinitionEntity definition = formInstance == null
                ? findDefinition(task.getProcessDefinitionId(), runtimeVariables)
                : null;
        fillForm(vo, formInstance, definition, runtimeVariables);
        vo.setDesignerJson(findDesignerJson(task.getProcessDefinitionId(), definition));
        Map<String, String> formPermissions = taskFormPermissions(task, vo.getFormJson());
        vo.setFormPermissions(WorkflowJsonVO.of(formPermissions));
        vo.setRenderConfig(renderConfig(task, formInstance, formPermissions));
        vo.setRecords(records(task.getProcessInstanceId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveDraft(SaveWorkflowTaskDraftCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        saveDraftWithResult(command);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowTaskActionResultVO saveDraftWithResult(SaveWorkflowTaskDraftCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID, "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        WorkflowNodeActionConfigVO action = ensureActionEnabled(task, "save");
        ensureCommentIfRequired(action, command.getComment());
        ensureCurrentUserCanOperate(task);

        Map<String, Object> variables = mergeVariables(task.getProcessInstanceId(), json(command.getVariables()));
        runtimeService.setVariables(task.getProcessInstanceId(), variables);
        updateFormInstance(task.getProcessInstanceId(), variables, WorkflowInstanceStatus.RUNNING);
        saveRecord(task, WorkflowTaskAction.SAVE, command.getComment(), json(command.getVariables()));
        WorkflowBusinessApplyVO apply = workflowBusinessApplyService.refreshCurrentTasksAndReturn(task.getProcessInstanceId());
        WorkflowFormInstanceEntity formInstance = findFormInstance(task.getProcessInstanceId());
        workflowEventPublisher.publishTaskSaved(task, formInstance, variables, command.getComment(), apply);
        return toActionResult(WorkflowTaskAction.SAVE, task, false, apply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean complete(CompleteWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        completeWithResult(command);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowTaskCompleteResultVO completeWithResult(CompleteWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID, "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        WorkflowNodeActionConfigVO action = ensureActionEnabled(task, "complete");
        ensureCommentIfRequired(action, command.getComment());
        ensureCurrentUserCanOperate(task);

        Map<String, Object> variables = mergeVariables(task.getProcessInstanceId(), json(command.getVariables()));
        if (StringUtils.hasText(command.getComment())) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), command.getComment().trim());
        }
        claimIfUnassigned(task);
        taskService.complete(task.getId(), variables);
        saveRecord(task, WorkflowTaskAction.COMPLETE, command.getComment(), json(command.getVariables()));
        boolean ended = isProcessEnded(task.getProcessInstanceId());
        updateFormInstance(task.getProcessInstanceId(), variables,
                ended ? WorkflowInstanceStatus.COMPLETED : WorkflowInstanceStatus.RUNNING);
        WorkflowFormInstanceEntity formInstance = findFormInstance(task.getProcessInstanceId());
        workflowEventPublisher.publishTaskCompleted(task, formInstance, variables, command.getComment());
        if (ended) {
            workflowBusinessApplyService.markApproved(task.getProcessInstanceId());
            WorkflowBusinessApplyVO apply = workflowBusinessApplyService.findByProcessInstance(task.getProcessInstanceId());
            workflowEventPublisher.publishProcessCompleted(task.getProcessInstanceId(), formInstance, variables, apply);
        }
        triggerEventNotify(task, variables);
        WorkflowTaskAdvanceResult advanceResult = advanceRuntimeTasks(task.getProcessInstanceId());
        workflowEventPublisher.publishTaskAdvanced(
                task,
                formInstance,
                variables,
                command.getComment(),
                advanceResult.ended(),
                advanceResult.businessApply());
        return toCompleteResult(task, advanceResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reject(RejectWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        rejectWithResult(command);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowTaskActionResultVO rejectWithResult(RejectWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID, "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        WorkflowNodeActionConfigVO action = ensureActionEnabled(task, "reject");
        ensureCommentIfRequired(action, command.getComment());
        ensureCurrentUserCanOperate(task);

        Map<String, Object> variables = mergeVariables(task.getProcessInstanceId(), json(command.getVariables()));
        if (StringUtils.hasText(command.getComment())) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), command.getComment().trim());
        }
        claimIfUnassigned(task);
        saveRecord(task, WorkflowTaskAction.REJECT, command.getComment(), json(command.getVariables()));
        triggerEventNotify(task, variables);
        runtimeService.deleteProcessInstance(task.getProcessInstanceId(),
                StringUtils.hasText(command.getComment()) ? command.getComment().trim() : DEFAULT_REJECT_REASON);
        updateFormInstance(task.getProcessInstanceId(), variables, WorkflowInstanceStatus.REJECTED);
        WorkflowFormInstanceEntity formInstance = findFormInstance(task.getProcessInstanceId());
        String reason = StringUtils.hasText(command.getComment()) ? command.getComment().trim() : DEFAULT_REJECT_REASON;
        workflowEventPublisher.publishTaskRejected(task, formInstance, variables, command.getComment());
        workflowBusinessApplyService.markRejected(new WorkflowTaskStatusContext(
                task.getProcessInstanceId(), reason, task.getId(), task.getTaskDefinitionKey()));
        WorkflowBusinessApplyVO apply = workflowBusinessApplyService.findByProcessInstance(task.getProcessInstanceId());
        workflowEventPublisher.publishProcessRejected(task.getProcessInstanceId(), formInstance, variables, reason, apply);
        workflowEventPublisher.publishProcessEnded(task.getProcessInstanceId(), formInstance, variables, reason, apply);
        return toActionResult(WorkflowTaskAction.REJECT, task, true, apply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowTaskCompleteResultVO returnTask(ReturnWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID, "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        WorkflowNodeActionConfigVO action = ensureActionEnabled(task, "returnTask");
        ensureCommentIfRequired(action, command.getComment());
        ensureCurrentUserCanOperate(task);

        String targetTaskDefinitionKey = resolveReturnTarget(task, command.getTargetTaskDefinitionKey());
        Map<String, Object> variables = mergeVariables(task.getProcessInstanceId(), json(command.getVariables()));
        if (StringUtils.hasText(command.getComment())) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), command.getComment().trim());
        }
        claimIfUnassigned(task);
        runtimeService.setVariables(task.getProcessInstanceId(), variables);
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(task.getProcessInstanceId())
                .moveActivityIdTo(task.getTaskDefinitionKey(), targetTaskDefinitionKey)
                .changeState();
        saveRecord(task, WorkflowTaskAction.RETURN, command.getComment(),
                returnVariables(json(command.getVariables()), targetTaskDefinitionKey));
        updateFormInstance(task.getProcessInstanceId(), variables, WorkflowInstanceStatus.RUNNING);
        triggerEventNotify(task, variables);
        WorkflowTaskAdvanceResult advanceResult = advanceRuntimeTasks(task.getProcessInstanceId());
        WorkflowFormInstanceEntity formInstance = findFormInstance(task.getProcessInstanceId());
        workflowEventPublisher.publishTaskAdvanced(
                task,
                formInstance,
                variables,
                command.getComment(),
                advanceResult.ended(),
                advanceResult.businessApply());
        return toCompleteResult(task, advanceResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean transfer(TransferWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID, "任务ID不能为空");
        Require.notBlank(command.getTargetUserId(), WorkflowCode.TASK_INVALID, "目标办理人不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        WorkflowNodeActionConfigVO action = ensureActionEnabled(task, "transfer");
        ensureCommentIfRequired(action, command.getComment());
        ensureCurrentUserCanOperate(task);
        String target = command.getTargetUserId().trim();
        ensureNotCurrentUser(target, "不能转办给自己");
        taskService.setAssignee(task.getId(), target);
        task.setAssignee(target);
        saveRecord(task, WorkflowTaskAction.TRANSFER, command.getComment(), Map.of("targetUserId", target));
        workflowBusinessApplyService.refreshCurrentTasks(task.getProcessInstanceId());
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addSign(AddSignWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID, "任务ID不能为空");
        Require.notEmpty(command.getTargetUserIds(), WorkflowCode.TASK_INVALID, "加签办理人不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        WorkflowNodeActionConfigVO action = ensureActionEnabled(task, "addSign");
        ensureCommentIfRequired(action, command.getComment());
        ensureCurrentUserCanOperate(task);
        List<String> targets = command.getTargetUserIds().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Require.notEmpty(targets, WorkflowCode.TASK_INVALID, "加签办理人不能为空");
        for (String target : targets) {
            ensureNotCurrentUser(target, "不能加签给自己");
            runtimeService.addMultiInstanceExecution(task.getTaskDefinitionKey(), task.getProcessInstanceId(),
                    Map.of("mangoAssignee_" + task.getTaskDefinitionKey(), target));
        }
        saveRecord(task, WorkflowTaskAction.ADD_SIGN, command.getComment(), Map.of("targetUserIds", targets));
        workflowBusinessApplyService.refreshCurrentTasks(task.getProcessInstanceId());
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean claim(ClaimWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        claimWithResult(command);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowTaskActionResultVO claimWithResult(ClaimWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID, "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        Require.isTrue(!StringUtils.hasText(task.getAssignee()), WorkflowCode.TASK_INVALID, "任务已被认领");
        ensureCurrentUserCanOperate(task);
        taskService.claim(task.getId(), currentUser());
        taskService.setVariableLocal(task.getId(), CLAIMED_FROM_CANDIDATE_VARIABLE, Boolean.TRUE);
        task.setAssignee(currentUser());
        saveRecord(task, WorkflowTaskAction.CLAIM, "认领任务", Map.of());
        WorkflowBusinessApplyVO apply = workflowBusinessApplyService.refreshCurrentTasksAndReturn(task.getProcessInstanceId());
        WorkflowFormInstanceEntity formInstance = findFormInstance(task.getProcessInstanceId());
        workflowEventPublisher.publishTaskClaimed(task, formInstance, readStoredVariables(task.getProcessInstanceId()), apply);
        return toActionResult(WorkflowTaskAction.CLAIM, task, false, apply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unclaim(ClaimWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        unclaimWithResult(command);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowTaskActionResultVO unclaimWithResult(ClaimWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID, "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        Require.isTrue(currentUser().equals(task.getAssignee()), WorkflowCode.TASK_INVALID, "只能释放自己认领的任务");
        Require.isTrue(isClaimedFromCandidate(task.getId()), WorkflowCode.TASK_INVALID, "只能释放通过认领获得的任务");
        taskService.unclaim(task.getId());
        taskService.removeVariableLocal(task.getId(), CLAIMED_FROM_CANDIDATE_VARIABLE);
        task.setAssignee(null);
        saveRecord(task, WorkflowTaskAction.UNCLAIM, "释放任务", Map.of());
        WorkflowBusinessApplyVO apply = workflowBusinessApplyService.refreshCurrentTasksAndReturn(task.getProcessInstanceId());
        WorkflowFormInstanceEntity formInstance = findFormInstance(task.getProcessInstanceId());
        workflowEventPublisher.publishTaskUnclaimed(task, formInstance, readStoredVariables(task.getProcessInstanceId()), apply);
        return toActionResult(WorkflowTaskAction.UNCLAIM, task, false, apply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean readCopied(ReadWorkflowCopiedTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notNull(command.getCopiedTaskId(), WorkflowCode.TASK_INVALID, "抄送记录ID不能为空");
        WorkflowCopiedTaskEntity copiedTask = copiedTaskMapper.selectById(command.getCopiedTaskId());
        Require.notNull(copiedTask, WorkflowCode.TASK_NOT_FOUND);
        Require.isTrue(currentUser().equals(copiedTask.getCopiedUserId()), WorkflowCode.TASK_INVALID, "只能标记自己的抄送记录");
        LocalDateTime now = LocalDateTime.now();
        copiedTask.setReadFlag(Boolean.TRUE);
        copiedTask.setReadTime(now);
        copiedTask.setUpdatedBy(MangoContextHolder.userId());
        copiedTask.setUpdatedAt(now);
        copiedTaskMapper.updateById(copiedTask);
        saveCopiedRecord(copiedTask, WorkflowTaskAction.READ, "抄送已阅", Map.of("copiedTaskId", copiedTask.getId()));
        return Boolean.TRUE;
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleWorkflowNodeExecution(WorkflowNodeExecutionEvent event) {
        if (event == null || event.getContext() == null || !"CC".equals(event.getContext().getNodeType())) {
            return;
        }
        Map<String, Object> properties = event.getContext().getProperties();
        Object ccConfigValue = properties == null ? null : properties.get("ccConfig");
        if (!(ccConfigValue instanceof Map<?, ?> ccConfig)) {
            return;
        }
        List<String> userIds = valueList(ccConfig.get("userIds"));
        if (userIds.isEmpty()) {
            return;
        }
        String processInstanceId = event.getContext().getExecution().getProcessInstanceId();
        WorkflowProcessInstanceVO process = processInfo(processInstanceId);
        Map<String, Object> variables = readStoredVariables(processInstanceId);
        for (String userId : userIds) {
            if (!StringUtils.hasText(userId)) {
                continue;
            }
            WorkflowCopiedTaskEntity copiedTask = new WorkflowCopiedTaskEntity();
            copiedTask.setTenantId(currentTenantId());
            copiedTask.setProcessInstanceId(processInstanceId);
            copiedTask.setProcessDefinitionId(process.getProcessDefinitionId());
            copiedTask.setProcessName(process.getProcessName());
            copiedTask.setProcessKey(process.getProcessKey());
            copiedTask.setBusinessKey(process.getBusinessKey());
            copiedTask.setNodeDefinitionKey(event.getContext().getNodeDefinitionCode());
            copiedTask.setNodeName(event.getContext().getNodeName());
            copiedTask.setCopiedUserId(userId);
            copiedTask.setCopiedUserName(userId);
            Object messageTemplate = ccConfig.get("messageTemplate");
            copiedTask.setMessage(renderCcMessage(messageTemplate == null ? "" : String.valueOf(messageTemplate), variables, process));
            copiedTask.setReadFlag(Boolean.FALSE);
            LocalDateTime now = LocalDateTime.now();
            copiedTask.setCreatedTime(now);
            copiedTask.setCreatedAt(now);
            copiedTask.setUpdatedAt(now);
            copiedTaskMapper.insert(copiedTask);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowTaskAdvanceResult advanceRuntimeTasks(String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId)) {
            return new WorkflowTaskAdvanceResult(processInstanceId, false, null);
        }
        if (isProcessEnded(processInstanceId)) {
            return new WorkflowTaskAdvanceResult(processInstanceId, true,
                    workflowBusinessApplyService.findByProcessInstance(processInstanceId));
        }
        for (int i = 0; i < 16; i++) {
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
            boolean changed = false;
            for (Task task : tasks) {
                changed = resolveRuntimeTask(task) || changed;
            }
            updateFormInstance(processInstanceId, readStoredVariables(processInstanceId),
                    isProcessEnded(processInstanceId) ? WorkflowInstanceStatus.COMPLETED : WorkflowInstanceStatus.RUNNING);
            WorkflowBusinessApplyVO businessApply = workflowBusinessApplyService.refreshCurrentTasksAndReturn(processInstanceId);
            if (!changed || isProcessEnded(processInstanceId)) {
                if (isProcessEnded(processInstanceId)) {
                    WorkflowFormInstanceEntity formInstance = findFormInstance(processInstanceId);
                    if (formInstance != null && WorkflowInstanceStatus.COMPLETED.name().equals(formInstance.getStatus())) {
                        workflowBusinessApplyService.markApproved(processInstanceId);
                        businessApply = workflowBusinessApplyService.findByProcessInstance(processInstanceId);
                    }
                }
                return new WorkflowTaskAdvanceResult(processInstanceId, isProcessEnded(processInstanceId), businessApply);
            }
        }
        return new WorkflowTaskAdvanceResult(processInstanceId, isProcessEnded(processInstanceId),
                workflowBusinessApplyService.findByProcessInstance(processInstanceId));
    }

    private WorkflowTaskCompleteResultVO toCompleteResult(Task completedTask, WorkflowTaskAdvanceResult advanceResult) {
        WorkflowTaskCompleteResultVO vo = new WorkflowTaskCompleteResultVO();
        vo.setActionResult(WorkflowTaskAction.COMPLETE);
        vo.setCompletedTaskId(completedTask.getId());
        vo.setCompletedTaskName(completedTask.getName());
        vo.setCompletedTaskDefinitionKey(completedTask.getTaskDefinitionKey());
        vo.setProcessInstanceId(advanceResult.processInstanceId());
        vo.setEnded(advanceResult.ended());
        WorkflowBusinessApplyVO apply = advanceResult.businessApply();
        if (apply == null) {
            vo.setCurrentTasks(List.of());
            return vo;
        }
        vo.setApplyId(apply.getId());
        vo.setBusinessType(apply.getBusinessType());
        vo.setBusinessKey(apply.getBusinessKey());
        vo.setApplyStatus(apply.getApplyStatus());
        vo.setApplyStatusName(apply.getApplyStatusName());
        vo.setCurrentTaskNames(apply.getCurrentTaskNames());
        vo.setCurrentTaskDefinitionKeys(apply.getCurrentTaskDefinitionKeys());
        vo.setCurrentAssigneeNames(apply.getCurrentAssigneeNames());
        List<WorkflowBusinessApplyCurrentTaskVO> currentTasks = apply.getCurrentTasks() == null
                ? List.<WorkflowBusinessApplyCurrentTaskVO>of()
                : apply.getCurrentTasks();
        vo.setCurrentTasks(currentTasks);
        vo.setCancelled(WorkflowApplyStatus.CANCELED == apply.getApplyStatus());
        vo.setRejected(WorkflowApplyStatus.REJECTED == apply.getApplyStatus());
        fillCurrentTask(vo, currentTasks);
        return vo;
    }

    private WorkflowTaskActionResultVO toActionResult(WorkflowTaskAction action, Task previousTask,
                                                      boolean ended, WorkflowBusinessApplyVO apply) {
        WorkflowTaskActionResultVO vo = new WorkflowTaskActionResultVO();
        vo.setActionResult(action);
        vo.setPreviousTaskId(previousTask.getId());
        vo.setPreviousTaskDefinitionKey(previousTask.getTaskDefinitionKey());
        vo.setPreviousTaskName(previousTask.getName());
        vo.setProcessInstanceId(previousTask.getProcessInstanceId());
        vo.setEnded(ended);
        if (apply == null) {
            vo.setNextTasks(List.of());
            vo.setCandidateUsers(List.of());
            vo.setCandidateGroups(List.of());
            vo.setClaimStatus(WorkflowTaskClaimStatus.NONE);
            return vo;
        }
        vo.setApplyId(apply.getId());
        vo.setBusinessType(apply.getBusinessType());
        vo.setBusinessKey(apply.getBusinessKey());
        vo.setProcessStatus(apply.getApplyStatus());
        vo.setProcessStatusName(apply.getApplyStatusName());
        vo.setCancelled(WorkflowApplyStatus.CANCELED == apply.getApplyStatus());
        vo.setRejected(WorkflowApplyStatus.REJECTED == apply.getApplyStatus());
        List<WorkflowBusinessApplyCurrentTaskVO> nextTasks = apply.getCurrentTasks() == null
                ? List.<WorkflowBusinessApplyCurrentTaskVO>of()
                : apply.getCurrentTasks();
        vo.setNextTasks(nextTasks);
        fillCurrentTask(vo, nextTasks);
        return vo;
    }

    private void fillCurrentTask(WorkflowTaskCompleteResultVO vo, List<WorkflowBusinessApplyCurrentTaskVO> currentTasks) {
        if (currentTasks.isEmpty()) {
            vo.setClaimStatus(WorkflowTaskClaimStatus.NONE);
            vo.setCandidateUsers(List.of());
            vo.setCandidateGroups(List.of());
            return;
        }
        WorkflowBusinessApplyCurrentTaskVO first = currentTasks.getFirst();
        vo.setCurrentTaskId(first.getTaskId());
        vo.setCurrentTaskName(first.getTaskName());
        vo.setTaskDefinitionKey(first.getTaskDefinitionKey());
        vo.setAssigneeId(first.getAssigneeId());
        vo.setAssigneeName(first.getAssigneeName());
        vo.setClaimStatus(first.getClaimStatus());
        vo.setCandidateUsers(first.getCandidateUsers() == null ? List.of() : first.getCandidateUsers());
        vo.setCandidateGroups(first.getCandidateGroups() == null ? List.of() : first.getCandidateGroups());
        vo.setCurrentTask(first);
    }

    private void fillCurrentTask(WorkflowTaskActionResultVO vo, List<WorkflowBusinessApplyCurrentTaskVO> currentTasks) {
        if (currentTasks.isEmpty()) {
            vo.setClaimStatus(WorkflowTaskClaimStatus.NONE);
            vo.setCandidateUsers(List.of());
            vo.setCandidateGroups(List.of());
            return;
        }
        WorkflowBusinessApplyCurrentTaskVO first = currentTasks.getFirst();
        vo.setCurrentTaskId(first.getTaskId());
        vo.setCurrentTaskName(first.getTaskName());
        vo.setTaskDefinitionKey(first.getTaskDefinitionKey());
        vo.setAssigneeId(first.getAssigneeId());
        vo.setAssigneeName(first.getAssigneeName());
        vo.setClaimStatus(first.getClaimStatus());
        vo.setCandidateUsers(first.getCandidateUsers() == null ? List.of() : first.getCandidateUsers());
        vo.setCandidateGroups(first.getCandidateGroups() == null ? List.of() : first.getCandidateGroups());
        vo.setCurrentTask(first);
    }

    private String resolveReturnTarget(Task task, String configuredTarget) {
        String target = trim(configuredTarget);
        if (StringUtils.hasText(target)) {
            ensureHistoricTarget(task, target);
            return target;
        }
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();
        for (HistoricTaskInstance historicTask : historicTasks) {
            String taskDefinitionKey = trim(historicTask.getTaskDefinitionKey());
            if (StringUtils.hasText(taskDefinitionKey) && !taskDefinitionKey.equals(task.getTaskDefinitionKey())) {
                return taskDefinitionKey;
            }
        }
        Require.isTrue(false, WorkflowCode.TASK_INVALID, "没有可退回的历史节点");
        return "";
    }

    private void ensureHistoricTarget(Task task, String targetTaskDefinitionKey) {
        long count = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .taskDefinitionKey(targetTaskDefinitionKey)
                .finished()
                .count();
        Require.isTrue(count > 0, WorkflowCode.TASK_INVALID, "目标退回节点不在已完成历史中");
        Require.isTrue(!targetTaskDefinitionKey.equals(task.getTaskDefinitionKey()),
                WorkflowCode.TASK_INVALID, "不能退回当前节点");
    }

    private Map<String, Object> returnVariables(Map<String, Object> variables, String targetTaskDefinitionKey) {
        Map<String, Object> result = new LinkedHashMap<>(variables == null ? Map.of() : variables);
        result.put("targetTaskDefinitionKey", targetTaskDefinitionKey);
        return result;
    }

    @Override
    public WorkflowProcessDetailVO processDetail(String processInstanceId) {
        Require.notBlank(processInstanceId, WorkflowCode.PROCESS_INSTANCE_NOT_FOUND, "流程实例ID不能为空");
        WorkflowProcessDetailVO vo = new WorkflowProcessDetailVO();
        vo.setProcess(processInfo(processInstanceId));
        WorkflowFormInstanceEntity formInstance = findFormInstance(processInstanceId);
        Map<String, Object> runtimeVariables = formInstance == null ? readRuntimeVariables(processInstanceId) : Map.of();
        WorkflowDefinitionEntity definition = formInstance == null
                ? findDefinition(vo.getProcess().getProcessDefinitionId(), runtimeVariables)
                : null;
        vo.setFormCode(formInstance == null ? (definition == null ? null : definition.getFormCode()) : formInstance.getFormCode());
        vo.setFormJson(formInstance == null ? (definition == null ? null : definition.getFormJson()) : formInstance.getFormJson());
        vo.setDesignerJson(findDesignerJson(vo.getProcess().getProcessDefinitionId(), definition));
        vo.setVariables(WorkflowJsonVO.of(formInstance == null ? runtimeVariables : parseMap(formInstance.getVariablesJson())));
        vo.setRenderConfig(renderConfig(processInstanceId, null, formInstance, Map.of()));
        vo.setRecords(records(processInstanceId));
        return vo;
    }

    private WorkflowTaskPageQuery resolve(WorkflowTaskPageQuery query) {
        return query == null ? new WorkflowTaskPageQuery() : query;
    }

    private List<String> businessProcessInstanceIds(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return businessApplyMapper.selectList(new LambdaQueryWrapper<WorkflowBusinessApplyEntity>()
                        .select(WorkflowBusinessApplyEntity::getProcessInstanceId)
                        .isNotNull(WorkflowBusinessApplyEntity::getProcessInstanceId)
                        .and(wrapper -> wrapper
                                .like(WorkflowBusinessApplyEntity::getBusinessKey, keyword)
                                .or()
                                .like(WorkflowBusinessApplyEntity::getApplyTitle, keyword)
                                .or()
                                .like(WorkflowBusinessApplyEntity::getProcessName, keyword)
                                .or()
                                .like(WorkflowBusinessApplyEntity::getCurrentTaskNames, keyword)))
                .stream()
                .map(WorkflowBusinessApplyEntity::getProcessInstanceId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private void fillForm(WorkflowTaskDetailVO vo, WorkflowFormInstanceEntity formInstance, WorkflowDefinitionEntity definition,
                          Map<String, Object> runtimeVariables) {
        vo.setFormCode(formInstance == null ? (definition == null ? null : definition.getFormCode()) : formInstance.getFormCode());
        vo.setFormJson(formInstance == null ? (definition == null ? null : definition.getFormJson()) : formInstance.getFormJson());
        vo.setVariables(WorkflowJsonVO.of(formInstance == null ? runtimeVariables : parseMap(formInstance.getVariablesJson())));
    }

    private WorkflowDefinitionEntity findDefinition(String processDefinitionId, Map<String, Object> variables) {
        if (StringUtils.hasText(processDefinitionId)) {
            WorkflowDefinitionEntity definition = definitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                    .eq(WorkflowDefinitionEntity::getProcessDefinitionId, processDefinitionId)
                    .last("limit 1"));
            if (definition != null) {
                return definition;
            }
        }
        Long definitionId = variableLong(variables, "mangoDefinitionId");
        return definitionId == null ? null : definitionMapper.selectById(definitionId);
    }

    private String findDesignerJson(String processDefinitionId, WorkflowDefinitionEntity resolvedDefinition) {
        if (!StringUtils.hasText(processDefinitionId)) {
            return null;
        }
        WorkflowDefinitionVersionEntity version = definitionVersionMapper.selectOne(
                new LambdaQueryWrapper<WorkflowDefinitionVersionEntity>()
                        .eq(WorkflowDefinitionVersionEntity::getProcessDefinitionId, processDefinitionId)
                        .orderByDesc(WorkflowDefinitionVersionEntity::getId)
                        .last("limit 1"));
        if (version != null && StringUtils.hasText(version.getDesignerJson())) {
            return version.getDesignerJson();
        }
        WorkflowDefinitionEntity definition = resolvedDefinition;
        if (definition == null || !processDefinitionId.equals(definition.getProcessDefinitionId())) {
            definition = definitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                    .eq(WorkflowDefinitionEntity::getProcessDefinitionId, processDefinitionId)
                    .last("limit 1"));
        }
        return definition == null || !processDefinitionId.equals(definition.getProcessDefinitionId())
                ? null
                : definition.getDesignerJson();
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
        WorkflowFormInstanceEntity formInstance = findFormInstance(processInstanceId);
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
        WorkflowFormInstanceEntity formInstance = findFormInstance(processInstanceId);
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
        fillClaimState(vo, task);
        vo.setStatus(WorkflowTaskRuntimeStatus.TODO.getLabel());
        if (task.getCreateTime() != null) {
            vo.setCreateTime(task.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        WorkflowFormInstanceEntity formInstance = findFormInstance(task.getProcessInstanceId());
        WorkflowBusinessApplyVO apply = workflowBusinessApplyService.findByProcessInstance(task.getProcessInstanceId());
        if (formInstance != null) {
            vo.setBusinessKey(formInstance.getBusinessKey());
            vo.setProcessName(formInstance.getDefinitionName());
            vo.setProcessKey(formInstance.getDefinitionKey());
        } else if (apply != null) {
            vo.setBusinessKey(apply.getBusinessKey());
            vo.setProcessName(apply.getProcessName());
            vo.setProcessKey(apply.getProcessDefinitionKey());
        } else {
            fillProcessFallback(vo, task.getProcessInstanceId(), task.getProcessDefinitionId());
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
        vo.setClaimable(Boolean.FALSE);
        vo.setUnclaimable(Boolean.FALSE);
        vo.setStatus(WorkflowTaskRuntimeStatus.DONE.getLabel());
        if (task.getCreateTime() != null) {
            vo.setCreateTime(task.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        if (task.getEndTime() != null) {
            vo.setEndTime(task.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        WorkflowFormInstanceEntity formInstance = findFormInstance(task.getProcessInstanceId());
        WorkflowBusinessApplyVO apply = workflowBusinessApplyService.findByProcessInstance(task.getProcessInstanceId());
        if (formInstance != null) {
            vo.setBusinessKey(formInstance.getBusinessKey());
            vo.setProcessName(formInstance.getDefinitionName());
            vo.setProcessKey(formInstance.getDefinitionKey());
        } else if (apply != null) {
            vo.setBusinessKey(apply.getBusinessKey());
            vo.setProcessName(apply.getProcessName());
            vo.setProcessKey(apply.getProcessDefinitionKey());
        } else {
            fillProcessFallback(vo, task.getProcessInstanceId(), task.getProcessDefinitionId());
        }
        return vo;
    }

    private WorkflowTaskVO fromCopiedTask(WorkflowCopiedTaskEntity copiedTask) {
        WorkflowTaskVO vo = new WorkflowTaskVO();
        vo.setId(String.valueOf(copiedTask.getId()));
        vo.setTaskName(copiedTask.getNodeName());
        vo.setTaskDefinitionKey(copiedTask.getNodeDefinitionKey());
        vo.setProcessInstanceId(copiedTask.getProcessInstanceId());
        vo.setProcessDefinitionId(copiedTask.getProcessDefinitionId());
        vo.setBusinessKey(copiedTask.getBusinessKey());
        vo.setProcessName(copiedTask.getProcessName());
        vo.setProcessKey(copiedTask.getProcessKey());
        vo.setAssigneeName(copiedTask.getCopiedUserName());
        vo.setClaimable(Boolean.FALSE);
        vo.setUnclaimable(Boolean.FALSE);
        vo.setStatus(Boolean.TRUE.equals(copiedTask.getReadFlag()) ? "已阅" : "待阅");
        vo.setCreateTime(copiedTask.getCreatedTime());
        vo.setEndTime(copiedTask.getReadTime());
        return vo;
    }

    private void fillClaimState(WorkflowTaskVO vo, Task task) {
        boolean assigned = StringUtils.hasText(task.getAssignee());
        boolean candidate = !assigned && currentUserCanClaim(task);
        boolean claimedByCurrentUser = assigned
                && currentUser().equals(task.getAssignee())
                && isClaimedFromCandidate(task.getId());
        vo.setClaimable(candidate);
        vo.setUnclaimable(claimedByCurrentUser);
    }

    private boolean currentUserCanClaim(Task task) {
        String currentUser = currentUser();
        if (taskService.createTaskQuery()
                .taskId(task.getId())
                .taskCandidateUser(currentUser)
                .count() > 0) {
            return true;
        }
        List<String> candidateGroups = candidateGroupProvider.currentCandidateGroups();
        return !candidateGroups.isEmpty() && taskService.createTaskQuery()
                .taskId(task.getId())
                .taskCandidateGroupIn(candidateGroups)
                .count() > 0;
    }

    private boolean isClaimedFromCandidate(String taskId) {
        return Boolean.TRUE.equals(taskService.getVariableLocal(taskId, CLAIMED_FROM_CANDIDATE_VARIABLE));
    }

    private void fillProcessFallback(WorkflowTaskVO vo, String processInstanceId, String processDefinitionId) {
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (instance == null) {
            vo.setProcessName(processDefinitionId);
            vo.setProcessKey(processDefinitionId);
            return;
        }
        vo.setBusinessKey(instance.getBusinessKey());
        vo.setProcessName(StringUtils.hasText(instance.getProcessDefinitionName())
                ? instance.getProcessDefinitionName()
                : processDefinitionId);
        vo.setProcessKey(StringUtils.hasText(instance.getProcessDefinitionKey())
                ? instance.getProcessDefinitionKey()
                : processDefinitionId);
    }

    private List<WorkflowTaskRecordVO> records(String processInstanceId) {
        return taskRecordMapper.selectList(new LambdaQueryWrapper<WorkflowTaskRecordEntity>()
                        .eq(WorkflowTaskRecordEntity::getProcessInstanceId, processInstanceId)
                        .orderByAsc(WorkflowTaskRecordEntity::getCreatedTime)
                        .orderByAsc(WorkflowTaskRecordEntity::getId))
                .stream()
                .map(this::fromRecord)
                .toList();
    }

    private WorkflowTaskRecordVO fromRecord(WorkflowTaskRecordEntity record) {
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
        vo.setVariables(WorkflowJsonVO.of(parseMap(record.getVariablesJson())));
        vo.setCreatedTime(record.getCreatedTime());
        return vo;
    }

    private WorkflowFormInstanceEntity findFormInstance(String processInstanceId) {
        return formInstanceMapper.selectOne(new LambdaQueryWrapper<WorkflowFormInstanceEntity>()
                .eq(WorkflowFormInstanceEntity::getProcessInstanceId, processInstanceId)
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
        WorkflowFormInstanceEntity formInstance = findFormInstance(processInstanceId);
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
        WorkflowTaskRecordEntity record = new WorkflowTaskRecordEntity();
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

    private void saveCopiedRecord(WorkflowCopiedTaskEntity copiedTask, WorkflowTaskAction action, String comment,
                                  Map<String, Object> variables) {
        WorkflowTaskRecordEntity record = new WorkflowTaskRecordEntity();
        record.setTenantId(currentTenantId());
        record.setProcessInstanceId(copiedTask.getProcessInstanceId());
        record.setTaskId(String.valueOf(copiedTask.getId()));
        record.setTaskName(copiedTask.getNodeName());
        record.setTaskDefinitionKey(copiedTask.getNodeDefinitionKey());
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
        formInstanceMapper.update(null, new UpdateWrapper<WorkflowFormInstanceEntity>()
                .eq("process_instance_id", processInstanceId)
                .set("variables_json", toJson(variables))
                .set("status", status.name())
                .set("updated_by", MangoContextHolder.userId())
                .set("updated_time", now)
                .set("updated_at", now));
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
        Require.isTrue(assigned || candidate || groupCandidate || unassignedAdmin, WorkflowCode.TASK_INVALID, "当前用户不能处理该任务");
    }

    private WorkflowNodeActionConfigVO ensureActionEnabled(Task task, String actionKey) {
        WorkflowApprovalNodeConfig config = taskApprovalConfig(task);
        var actions = WorkflowNodeActionConfigResolver.resolve(config);
        var action = actions.get(actionKey);
        Require.notNull(action, WorkflowCode.TASK_INVALID, "未知审批动作：" + actionKey);
        Require.isTrue(Boolean.TRUE.equals(action.getEnabled()), WorkflowCode.TASK_INVALID, "当前节点未启用该审批动作");
        Require.isTrue(!Boolean.TRUE.equals(action.getDisabled()), WorkflowCode.TASK_INVALID,
                StringUtils.hasText(action.getTooltip()) ? action.getTooltip() : "当前审批动作不可用");
        return action;
    }

    private void ensureCommentIfRequired(WorkflowNodeActionConfigVO action, String comment) {
        Require.isTrue(!Boolean.TRUE.equals(action.getRequireComment()) || StringUtils.hasText(comment),
                WorkflowCode.TASK_INVALID, "请填写审批意见");
    }

    private void ensureNotCurrentUser(String targetUser, String message) {
        Require.isTrue(!isCurrentUserIdentifier(targetUser), WorkflowCode.TASK_INVALID, message);
    }

    private boolean isCurrentUserIdentifier(String targetUser) {
        if (!StringUtils.hasText(targetUser)) {
            return false;
        }
        String target = targetUser.trim();
        String currentUser = currentUser();
        if (target.equals(currentUser)) {
            return true;
        }
        Long currentUserId = MangoContextHolder.userId();
        return currentUserId != null && target.equals(String.valueOf(currentUserId));
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
            return resolveEmptyRuntimeAssignee(task, config);
        }
        Map<String, Object> variables = readStoredVariables(task.getProcessInstanceId());
        WorkflowAssigneeResolver.ResolvedAssignees resolved = assigneeResolver.applyEmptyStrategy(config,
                assigneeResolver.resolve(config, variables, initiator(task.getProcessInstanceId()), task.getTaskDefinitionKey()),
                variables);
        if (resolved.empty()) {
            return applyAutoEmptyStrategy(task, resolved.emptyStrategy(), variables);
        }
        boolean usersChanged = applyResolvedRuntimeUsers(task, variables, resolved.users());
        boolean groupsChanged = applyResolvedRuntimeGroups(task, resolved.groups());
        return usersChanged || groupsChanged;
    }

    private boolean resolveEmptyRuntimeAssignee(Task task, WorkflowApprovalNodeConfig config) {
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

    private boolean applyResolvedRuntimeUsers(Task task, Map<String, Object> variables, List<String> users) {
        if (users == null || users.isEmpty()) {
            return false;
        }
        boolean changed = false;
        if (task.getAssignee() != null && task.getAssignee().startsWith("${mangoRuntimeAssignee_")) {
            taskService.setAssignee(task.getId(), users.get(0));
            changed = true;
        }
        String multiVariable = "mangoAssignees_" + task.getTaskDefinitionKey();
        if (!variables.containsKey(multiVariable)) {
            variables.put(multiVariable, users);
            runtimeService.setVariable(task.getProcessInstanceId(), multiVariable, users);
            changed = true;
        }
        return changed;
    }

    private boolean applyResolvedRuntimeGroups(Task task, List<String> groups) {
        if (groups == null || groups.isEmpty() || !taskIdentityGroups(task.getId()).isEmpty()) {
            return false;
        }
        for (String group : groups) {
            taskService.addCandidateGroup(task.getId(), group);
        }
        return true;
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
            WorkflowFormInstanceEntity formInstance = findFormInstance(task.getProcessInstanceId());
            String reason = "审批人为空，系统自动驳回";
            workflowEventPublisher.publishTaskRejected(task, formInstance, variables, reason);
            workflowBusinessApplyService.markRejected(new WorkflowTaskStatusContext(
                    task.getProcessInstanceId(), reason, task.getId(), task.getTaskDefinitionKey()));
            WorkflowBusinessApplyVO apply = workflowBusinessApplyService.findByProcessInstance(task.getProcessInstanceId());
            workflowEventPublisher.publishProcessRejected(task.getProcessInstanceId(), formInstance, variables, reason, apply);
            workflowEventPublisher.publishProcessEnded(task.getProcessInstanceId(), formInstance, variables, reason, apply);
            return true;
        }
        if (strategy == WorkflowEmptyAssigneeStrategy.AUTO_END) {
            saveRecord(task, WorkflowTaskAction.AUTO_END, "审批人为空，系统自动结束", variables);
            runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "审批人为空，系统自动结束");
            updateFormInstance(task.getProcessInstanceId(), variables, WorkflowInstanceStatus.ENDED);
            workflowBusinessApplyService.markTerminated(new WorkflowTaskStatusContext(
                    task.getProcessInstanceId(), "审批人为空，系统自动结束",
                    task.getId(), task.getTaskDefinitionKey()));
            WorkflowBusinessApplyVO apply = workflowBusinessApplyService.findByProcessInstance(task.getProcessInstanceId());
            workflowEventPublisher.publishProcessEnded(task.getProcessInstanceId(), findFormInstance(task.getProcessInstanceId()),
                    variables, "审批人为空，系统自动结束", apply);
            return true;
        }
        return false;
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

    private Map<String, String> taskFormPermissions(Task task, String formJson) {
        WorkflowApprovalNodeConfig config = taskApprovalConfig(task);
        Map<String, String> permissions = new LinkedHashMap<>();
        if (config != null && config.getFormPermissions() != null) {
            config.getFormPermissions().forEach((field, permission) ->
                    permissions.put(field, (permission == null ? WorkflowFormPermission.READONLY : permission).name()));
        }
        for (String field : formFields(formJson)) {
            permissions.putIfAbsent(field, WorkflowFormPermission.READONLY.name());
        }
        return permissions;
    }

    private List<String> formFields(String formJson) {
        if (!StringUtils.hasText(formJson)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(formJson);
            List<String> fields = new java.util.ArrayList<>();
            if (root.isArray()) {
                collectFormFields(root, fields);
            } else {
                JsonNode rules = root.get("rules");
                JsonNode customFields = root.get("fields");
                if (rules != null && rules.isArray()) {
                    collectFormFields(rules, fields);
                }
                if (customFields != null && customFields.isArray()) {
                    collectFormFields(customFields, fields);
                }
            }
            return fields.stream().distinct().toList();
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private void collectFormFields(JsonNode node, List<String> fields) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectFormFields(child, fields));
            return;
        }
        if (!node.isObject()) {
            return;
        }
        addFormField(node.get("field"), fields);
        addFormField(node.get("key"), fields);
        collectFormFields(node.get("children"), fields);
    }

    private void addFormField(JsonNode value, List<String> fields) {
        if (value == null || !value.isValueNode()) {
            return;
        }
        String field = value.asText();
        if (StringUtils.hasText(field) && !field.startsWith("__runtime_")) {
            fields.add(field);
        }
    }

    private WorkflowRenderConfigVO renderConfig(Task task, WorkflowFormInstanceEntity formInstance,
                                                Map<String, String> formPermissions) {
        return renderConfig(task.getProcessInstanceId(), task, formInstance, formPermissions);
    }

    private WorkflowRenderConfigVO renderConfig(String processInstanceId, Task task, WorkflowFormInstanceEntity formInstance,
                                                Map<String, String> formPermissions) {
        WorkflowBusinessApplyVO apply = workflowBusinessApplyService.findByProcessInstance(processInstanceId);
        Map<String, Object> variables = formInstance == null ? readStoredVariables(processInstanceId) : parseMap(formInstance.getVariablesJson());
        WorkflowRenderConfigVO vo = new WorkflowRenderConfigVO();
        vo.setProcessInstanceId(processInstanceId);
        vo.setBusinessType(apply == null ? variableString(variables, "businessType") : apply.getBusinessType());
        vo.setBusinessKey(apply == null ? (formInstance == null ? variableString(variables, "businessKey") : formInstance.getBusinessKey()) : apply.getBusinessKey());
        vo.setApplyId(apply == null ? variableLong(variables, "applyId") : apply.getId());
        vo.setRenderMode(resolveRenderMode(apply, variables));
        vo.setApplyPageKey(apply == null ? variableString(variables, "applyPageKey") : apply.getApplyPageKey());
        vo.setApprovePageKey(apply == null ? variableString(variables, "approvePageKey") : apply.getApprovePageKey());
        vo.setFormKey(apply == null ? (formInstance == null ? null : formInstance.getFormCode()) : apply.getFormKey());
        vo.setFormVersion(apply == null ? null : apply.getFormVersion());
        vo.setSnapshotRef(apply == null ? variableString(variables, "snapshotRef") : apply.getSnapshotRef());
        vo.setTaskDefinitionKey(task == null ? null : task.getTaskDefinitionKey());
        WorkflowApprovalNodeConfig config = taskApprovalConfig(task);
        vo.setNodeExtension(WorkflowJsonVO.of(config == null ? null : config.getExtension()));
        vo.setFormPermissions(WorkflowJsonVO.of(formPermissions));
        vo.setBusinessPermissions(WorkflowJsonVO.of(
                businessPermissions(variables, task == null ? null : task.getTaskDefinitionKey())));
        vo.setNodeActions(WorkflowJsonVO.of(WorkflowNodeActionConfigResolver.resolve(config)));
        return vo;
    }

    private Map<String, Object> json(WorkflowJsonRequest value) {
        return value == null ? Map.of() : value.toMap();
    }

    private WorkflowApplyRenderMode resolveRenderMode(WorkflowBusinessApplyVO apply, Map<String, Object> variables) {
        if (apply != null && apply.getRenderMode() != null) {
            return apply.getRenderMode();
        }
        String renderMode = variableString(variables, "renderMode");
        if (StringUtils.hasText(renderMode)) {
            WorkflowApplyRenderMode parsed = WorkflowApplyRenderMode.fromCode(renderMode);
            if (parsed != null) {
                return parsed;
            }
        }
        return StringUtils.hasText(variableString(variables, "businessType"))
                ? WorkflowApplyRenderMode.CUSTOM_PAGE
                : WorkflowApplyRenderMode.DYNAMIC_FORM;
    }

    private Map<String, Object> businessPermissions(Map<String, Object> variables, String taskDefinitionKey) {
        Object permissions = variables.get("businessPermissions");
        if (!(permissions instanceof Map<?, ?> map) || !StringUtils.hasText(taskDefinitionKey)) {
            return Map.of();
        }
        Object nodePermissions = map.get(taskDefinitionKey);
        if (!(nodePermissions instanceof Map<?, ?> nodeMap)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        nodeMap.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    private String variableString(Map<String, Object> variables, String key) {
        Object value = variables == null ? null : variables.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long variableLong(Map<String, Object> variables, String key) {
        Object value = variables == null ? null : variables.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (!StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private WorkflowApprovalNodeConfig taskApprovalConfig(Task task) {
        if (task == null || !StringUtils.hasText(task.getProcessDefinitionId()) || !StringUtils.hasText(task.getTaskDefinitionKey())) {
            return null;
        }
        BpmnModel model;
        try {
            model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        } catch (FlowableObjectNotFoundException e) {
            return Require.fail(WorkflowCode.TASK_INVALID, "当前任务引用的流程定义不存在，请清理测试脏数据后重试");
        }
        if (model == null) {
            return null;
        }
        FlowElement element = model.getFlowElement(task.getTaskDefinitionKey());
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

    private List<String> valueList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Iterable<?> iterable) {
            return toStringList(iterable);
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        return Arrays.stream(text.split("\\s*,\\s*"))
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<String> toStringList(Iterable<?> values) {
        List<String> result = new java.util.ArrayList<>();
        for (Object value : values) {
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                result.add(String.valueOf(value).trim());
            }
        }
        return result;
    }

    private String renderCcMessage(String template, Map<String, Object> variables, WorkflowProcessInstanceVO process) {
        String message = StringUtils.hasText(template) ? template : "流程抄送：" + process.getProcessName();
        Map<String, Object> context = new LinkedHashMap<>(variables == null ? Map.of() : variables);
        context.put("processName", process.getProcessName());
        context.put("processKey", process.getProcessKey());
        context.put("businessKey", process.getBusinessKey());
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            message = message.replace("${" + entry.getKey() + "}", entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return message;
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
