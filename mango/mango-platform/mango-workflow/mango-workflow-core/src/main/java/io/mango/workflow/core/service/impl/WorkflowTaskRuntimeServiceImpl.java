package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.WorkflowCode;
import io.mango.workflow.api.command.AddSignWorkflowTaskCommand;
import io.mango.workflow.api.command.ClaimWorkflowTaskCommand;
import io.mango.workflow.api.command.CompleteWorkflowTaskCommand;
import io.mango.workflow.api.command.ReadWorkflowCopiedTaskCommand;
import io.mango.workflow.api.command.RejectWorkflowTaskCommand;
import io.mango.workflow.api.command.SaveWorkflowTaskDraftCommand;
import io.mango.workflow.api.command.TransferWorkflowTaskCommand;
import io.mango.workflow.api.enums.WorkflowEmptyAssigneeStrategy;
import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.mango.workflow.api.enums.WorkflowFormPermission;
import io.mango.workflow.api.enums.WorkflowInstanceStatus;
import io.mango.workflow.api.enums.WorkflowTaskAction;
import io.mango.workflow.api.enums.WorkflowTaskRuntimeStatus;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessApplyCurrentTaskVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.api.vo.WorkflowMyTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowNodeActionConfigVO;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import io.mango.workflow.api.vo.WorkflowRenderConfigVO;
import io.mango.workflow.api.vo.WorkflowTaskCompleteResultVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.api.vo.WorkflowTaskRecordVO;
import io.mango.workflow.api.vo.WorkflowTaskSummaryVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import io.mango.workflow.core.engine.WorkflowAssigneeResolver;
import io.mango.workflow.core.engine.WorkflowAssigneeCollection;
import io.mango.workflow.core.engine.WorkflowCandidateGroupProvider;
import io.mango.workflow.core.engine.WorkflowNodeExecutionEvent;
import io.mango.workflow.core.entity.WorkflowBusinessApply;
import io.mango.workflow.core.entity.WorkflowCopiedTask;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.entity.WorkflowFormInstance;
import io.mango.workflow.core.entity.WorkflowTaskRecord;
import io.mango.workflow.core.event.WorkflowEventPublisher;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import io.mango.workflow.core.mapper.WorkflowCopiedTaskMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import io.mango.workflow.core.model.WorkflowApprovalNodeConfig;
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
@RequiredArgsConstructor
public class WorkflowTaskRuntimeServiceImpl implements IWorkflowTaskRuntimeService {

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
    private final WorkflowFormInstanceMapper formInstanceMapper;
    private final WorkflowTaskRecordMapper taskRecordMapper;
    private final ObjectMapper objectMapper;
    private final WorkflowAssigneeResolver assigneeResolver;
    private final WorkflowCandidateGroupProvider candidateGroupProvider;
    private final IWorkflowBusinessApplyService workflowBusinessApplyService;
    private final WorkflowEventPublisher workflowEventPublisher;

    @Override
    public R<PageResult<WorkflowTaskVO>> todo(WorkflowTaskPageQuery query) {
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
        return R.ok(PageResult.of(records, total, resolved.getPage(), resolved.getSize()));
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
    public R<WorkflowTaskSummaryVO> summary() {
        List<String> candidateGroups = candidateGroupProvider.currentCandidateGroups();
        WorkflowTaskSummaryVO vo = new WorkflowTaskSummaryVO();
        vo.setPendingApproval(countTodoByType("ASSIGNED", candidateGroups));
        vo.setPendingHandle(countTodoByType("CLAIMABLE", candidateGroups));
        vo.setPendingConfirm(countUnreadCopied());
        vo.setOverdue(countOverdueTasks(candidateGroups));
        return R.ok(vo);
    }

    @Override
    public R<WorkflowMyTaskSummaryVO> myTaskSummary() {
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
        return R.ok(vo);
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
        return copiedTaskMapper.selectCount(new LambdaQueryWrapper<WorkflowCopiedTask>()
                .eq(WorkflowCopiedTask::getCopiedUserId, currentUser())
                .eq(WorkflowCopiedTask::getReadFlag, Boolean.FALSE));
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
    public R<PageResult<WorkflowTaskVO>> done(WorkflowTaskPageQuery query) {
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
        return R.ok(PageResult.of(records, total, resolved.getPage(), resolved.getSize()));
    }

    @Override
    public R<PageResult<WorkflowTaskVO>> copied(WorkflowTaskPageQuery query) {
        WorkflowTaskPageQuery resolved = resolve(query);
        long offset = (resolved.getPage() - 1) * resolved.getSize();
        String keyword = trim(resolved.getKeyword());
        LambdaQueryWrapper<WorkflowCopiedTask> wrapper = new LambdaQueryWrapper<WorkflowCopiedTask>()
                .eq(WorkflowCopiedTask::getCopiedUserId, currentUser());
        if (Boolean.TRUE.equals(resolved.getUnread())) {
            wrapper.eq(WorkflowCopiedTask::getReadFlag, Boolean.FALSE);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item
                    .like(WorkflowCopiedTask::getProcessName, keyword)
                    .or()
                    .like(WorkflowCopiedTask::getBusinessKey, keyword)
                    .or()
                    .like(WorkflowCopiedTask::getNodeName, keyword)
                    .or()
                    .like(WorkflowCopiedTask::getMessage, keyword));
        }
        long total = copiedTaskMapper.selectCount(wrapper);
        List<WorkflowTaskVO> records = copiedTaskMapper.selectList(wrapper
                        .orderByDesc(WorkflowCopiedTask::getCreatedTime)
                        .last("limit " + offset + "," + resolved.getSize()))
                .stream()
                .map(this::fromCopiedTask)
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
        Map<String, Object> runtimeVariables = readRuntimeVariables(task.getProcessInstanceId());
        WorkflowDefinition definition = formInstance == null
                ? findDefinition(task.getProcessDefinitionId(), runtimeVariables)
                : null;
        fillForm(vo, formInstance, definition, runtimeVariables);
        vo.setFormPermissions(taskFormPermissions(task, vo.getFormJson()));
        vo.setRenderConfig(renderConfig(task, formInstance, vo.getFormPermissions()));
        vo.setRecords(records(task.getProcessInstanceId()));
        return R.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> saveDraft(SaveWorkflowTaskDraftCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID.getCode(), "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        WorkflowNodeActionConfigVO action = ensureActionEnabled(task, "save");
        ensureCommentIfRequired(action, command.getComment());
        ensureCurrentUserCanOperate(task);

        Map<String, Object> variables = mergeVariables(task.getProcessInstanceId(), command.getVariables());
        runtimeService.setVariables(task.getProcessInstanceId(), variables);
        updateFormInstance(task.getProcessInstanceId(), variables, WorkflowInstanceStatus.RUNNING);
        saveRecord(task, WorkflowTaskAction.SAVE, command.getComment(), command.getVariables());
        workflowBusinessApplyService.refreshCurrentTasks(task.getProcessInstanceId());
        return R.ok(Boolean.TRUE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> complete(CompleteWorkflowTaskCommand command) {
        completeWithResult(command);
        return R.ok(Boolean.TRUE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<WorkflowTaskCompleteResultVO> completeWithResult(CompleteWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID.getCode(), "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        WorkflowNodeActionConfigVO action = ensureActionEnabled(task, "complete");
        ensureCommentIfRequired(action, command.getComment());
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
        WorkflowFormInstance formInstance = findFormInstance(task.getProcessInstanceId());
        workflowEventPublisher.publishTaskCompleted(task, formInstance, variables, command.getComment());
        if (ended) {
            workflowEventPublisher.publishProcessCompleted(task.getProcessInstanceId(), formInstance, variables);
            workflowBusinessApplyService.markApproved(task.getProcessInstanceId());
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
        return R.ok(toCompleteResult(task, advanceResult));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> reject(RejectWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID.getCode(), "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        WorkflowNodeActionConfigVO action = ensureActionEnabled(task, "reject");
        ensureCommentIfRequired(action, command.getComment());
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
        WorkflowFormInstance formInstance = findFormInstance(task.getProcessInstanceId());
        String reason = StringUtils.hasText(command.getComment()) ? command.getComment().trim() : DEFAULT_REJECT_REASON;
        workflowEventPublisher.publishTaskRejected(task, formInstance, variables, command.getComment());
        workflowEventPublisher.publishProcessRejected(task.getProcessInstanceId(), formInstance, variables, reason);
        workflowEventPublisher.publishProcessEnded(task.getProcessInstanceId(), formInstance, variables, reason);
        workflowBusinessApplyService.markRejected(task.getProcessInstanceId(), reason, task.getId(), task.getTaskDefinitionKey());
        return R.ok(Boolean.TRUE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> transfer(TransferWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID.getCode(), "任务ID不能为空");
        Require.notBlank(command.getTargetUserId(), WorkflowCode.TASK_INVALID.getCode(), "目标办理人不能为空");
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
        return R.ok(Boolean.TRUE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> addSign(AddSignWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID.getCode(), "任务ID不能为空");
        Require.notEmpty(command.getTargetUserIds(), WorkflowCode.TASK_INVALID.getCode(), "加签办理人不能为空");
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
        Require.notEmpty(targets, WorkflowCode.TASK_INVALID.getCode(), "加签办理人不能为空");
        for (String target : targets) {
            ensureNotCurrentUser(target, "不能加签给自己");
            runtimeService.addMultiInstanceExecution(task.getTaskDefinitionKey(), task.getProcessInstanceId(),
                    Map.of("mangoAssignee_" + task.getTaskDefinitionKey(), target));
        }
        saveRecord(task, WorkflowTaskAction.ADD_SIGN, command.getComment(), Map.of("targetUserIds", targets));
        workflowBusinessApplyService.refreshCurrentTasks(task.getProcessInstanceId());
        return R.ok(Boolean.TRUE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> claim(ClaimWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID.getCode(), "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        Require.isTrue(!StringUtils.hasText(task.getAssignee()), WorkflowCode.TASK_INVALID.getCode(), "任务已被认领");
        ensureCurrentUserCanOperate(task);
        taskService.claim(task.getId(), currentUser());
        taskService.setVariableLocal(task.getId(), CLAIMED_FROM_CANDIDATE_VARIABLE, Boolean.TRUE);
        task.setAssignee(currentUser());
        saveRecord(task, WorkflowTaskAction.CLAIM, "认领任务", Map.of());
        workflowBusinessApplyService.refreshCurrentTasks(task.getProcessInstanceId());
        return R.ok(Boolean.TRUE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> unclaim(ClaimWorkflowTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notBlank(command.getTaskId(), WorkflowCode.TASK_INVALID.getCode(), "任务ID不能为空");
        Task task = taskService.createTaskQuery().taskId(command.getTaskId()).singleResult();
        Require.notNull(task, WorkflowCode.TASK_NOT_FOUND);
        Require.isTrue(currentUser().equals(task.getAssignee()), WorkflowCode.TASK_INVALID.getCode(), "只能释放自己认领的任务");
        Require.isTrue(isClaimedFromCandidate(task.getId()), WorkflowCode.TASK_INVALID.getCode(), "只能释放通过认领获得的任务");
        taskService.unclaim(task.getId());
        taskService.removeVariableLocal(task.getId(), CLAIMED_FROM_CANDIDATE_VARIABLE);
        task.setAssignee(null);
        saveRecord(task, WorkflowTaskAction.UNCLAIM, "释放任务", Map.of());
        workflowBusinessApplyService.refreshCurrentTasks(task.getProcessInstanceId());
        return R.ok(Boolean.TRUE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> readCopied(ReadWorkflowCopiedTaskCommand command) {
        Require.notNull(command, WorkflowCode.TASK_INVALID);
        Require.notNull(command.getCopiedTaskId(), WorkflowCode.TASK_INVALID.getCode(), "抄送记录ID不能为空");
        WorkflowCopiedTask copiedTask = copiedTaskMapper.selectById(command.getCopiedTaskId());
        Require.notNull(copiedTask, WorkflowCode.TASK_NOT_FOUND);
        Require.isTrue(currentUser().equals(copiedTask.getCopiedUserId()), WorkflowCode.TASK_INVALID.getCode(), "只能标记自己的抄送记录");
        LocalDateTime now = LocalDateTime.now();
        copiedTask.setReadFlag(Boolean.TRUE);
        copiedTask.setReadTime(now);
        copiedTask.setUpdatedBy(MangoContextHolder.userId());
        copiedTask.setUpdatedAt(now);
        copiedTaskMapper.updateById(copiedTask);
        saveCopiedRecord(copiedTask, WorkflowTaskAction.READ, "抄送已阅", Map.of("copiedTaskId", copiedTask.getId()));
        return R.ok(Boolean.TRUE);
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
            WorkflowCopiedTask copiedTask = new WorkflowCopiedTask();
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
                    WorkflowFormInstance formInstance = findFormInstance(processInstanceId);
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
        vo.setCompletedTaskId(completedTask.getId());
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
        vo.setCurrentTasks(apply.getCurrentTasks() == null
                ? List.<WorkflowBusinessApplyCurrentTaskVO>of()
                : apply.getCurrentTasks());
        return vo;
    }

    @Override
    public R<WorkflowProcessDetailVO> processDetail(String processInstanceId) {
        Require.notBlank(processInstanceId, WorkflowCode.PROCESS_INSTANCE_NOT_FOUND.getCode(), "流程实例ID不能为空");
        WorkflowProcessDetailVO vo = new WorkflowProcessDetailVO();
        vo.setProcess(processInfo(processInstanceId));
        WorkflowFormInstance formInstance = findFormInstance(processInstanceId);
        Map<String, Object> runtimeVariables = formInstance == null ? readRuntimeVariables(processInstanceId) : Map.of();
        WorkflowDefinition definition = formInstance == null
                ? findDefinition(vo.getProcess().getProcessDefinitionId(), runtimeVariables)
                : null;
        vo.setFormCode(formInstance == null ? (definition == null ? null : definition.getFormCode()) : formInstance.getFormCode());
        vo.setFormJson(formInstance == null ? (definition == null ? null : definition.getFormJson()) : formInstance.getFormJson());
        vo.setVariables(formInstance == null ? runtimeVariables : parseMap(formInstance.getVariablesJson()));
        vo.setRenderConfig(renderConfig(processInstanceId, null, formInstance, Map.of()));
        vo.setRecords(records(processInstanceId));
        return R.ok(vo);
    }

    private WorkflowTaskPageQuery resolve(WorkflowTaskPageQuery query) {
        return query == null ? new WorkflowTaskPageQuery() : query;
    }

    private List<String> businessProcessInstanceIds(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return businessApplyMapper.selectList(new LambdaQueryWrapper<WorkflowBusinessApply>()
                        .select(WorkflowBusinessApply::getProcessInstanceId)
                        .isNotNull(WorkflowBusinessApply::getProcessInstanceId)
                        .and(wrapper -> wrapper
                                .like(WorkflowBusinessApply::getBusinessKey, keyword)
                                .or()
                                .like(WorkflowBusinessApply::getApplyTitle, keyword)
                                .or()
                                .like(WorkflowBusinessApply::getProcessName, keyword)
                                .or()
                                .like(WorkflowBusinessApply::getCurrentTaskNames, keyword)))
                .stream()
                .map(WorkflowBusinessApply::getProcessInstanceId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private void fillForm(WorkflowTaskDetailVO vo, WorkflowFormInstance formInstance, WorkflowDefinition definition,
                          Map<String, Object> runtimeVariables) {
        vo.setFormCode(formInstance == null ? (definition == null ? null : definition.getFormCode()) : formInstance.getFormCode());
        vo.setFormJson(formInstance == null ? (definition == null ? null : definition.getFormJson()) : formInstance.getFormJson());
        vo.setVariables(formInstance == null ? runtimeVariables : parseMap(formInstance.getVariablesJson()));
    }

    private WorkflowDefinition findDefinition(String processDefinitionId, Map<String, Object> variables) {
        if (StringUtils.hasText(processDefinitionId)) {
            WorkflowDefinition definition = definitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinition>()
                    .eq(WorkflowDefinition::getProcessDefinitionId, processDefinitionId)
                    .last("limit 1"));
            if (definition != null) {
                return definition;
            }
        }
        Long definitionId = variableLong(variables, "mangoDefinitionId");
        return definitionId == null ? null : definitionMapper.selectById(definitionId);
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
        fillClaimState(vo, task);
        vo.setStatus(WorkflowTaskRuntimeStatus.TODO.getLabel());
        if (task.getCreateTime() != null) {
            vo.setCreateTime(task.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        WorkflowFormInstance formInstance = findFormInstance(task.getProcessInstanceId());
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
        WorkflowFormInstance formInstance = findFormInstance(task.getProcessInstanceId());
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

    private WorkflowTaskVO fromCopiedTask(WorkflowCopiedTask copiedTask) {
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

    private void saveCopiedRecord(WorkflowCopiedTask copiedTask, WorkflowTaskAction action, String comment,
                                  Map<String, Object> variables) {
        WorkflowTaskRecord record = new WorkflowTaskRecord();
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
        formInstanceMapper.update(null, new UpdateWrapper<WorkflowFormInstance>()
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
        Require.isTrue(assigned || candidate || groupCandidate || unassignedAdmin, WorkflowCode.TASK_INVALID.getCode(), "当前用户不能处理该任务");
    }

    private WorkflowNodeActionConfigVO ensureActionEnabled(Task task, String actionKey) {
        WorkflowApprovalNodeConfig config = taskApprovalConfig(task);
        var actions = WorkflowNodeActionConfigResolver.resolve(config);
        var action = actions.get(actionKey);
        Require.notNull(action, WorkflowCode.TASK_INVALID.getCode(), "未知审批动作：" + actionKey);
        Require.isTrue(Boolean.TRUE.equals(action.getEnabled()), WorkflowCode.TASK_INVALID.getCode(), "当前节点未启用该审批动作");
        Require.isTrue(!Boolean.TRUE.equals(action.getDisabled()), WorkflowCode.TASK_INVALID.getCode(),
                StringUtils.hasText(action.getTooltip()) ? action.getTooltip() : "当前审批动作不可用");
        return action;
    }

    private void ensureCommentIfRequired(WorkflowNodeActionConfigVO action, String comment) {
        Require.isTrue(!Boolean.TRUE.equals(action.getRequireComment()) || StringUtils.hasText(comment),
                WorkflowCode.TASK_INVALID.getCode(), "请填写审批意见");
    }

    private void ensureNotCurrentUser(String targetUser, String message) {
        Require.isTrue(!isCurrentUserIdentifier(targetUser), WorkflowCode.TASK_INVALID.getCode(), message);
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
            WorkflowFormInstance formInstance = findFormInstance(task.getProcessInstanceId());
            String reason = "审批人为空，系统自动驳回";
            workflowEventPublisher.publishTaskRejected(task, formInstance, variables, reason);
            workflowEventPublisher.publishProcessRejected(task.getProcessInstanceId(), formInstance, variables, reason);
            workflowEventPublisher.publishProcessEnded(task.getProcessInstanceId(), formInstance, variables, reason);
            workflowBusinessApplyService.markRejected(task.getProcessInstanceId(), reason, task.getId(), task.getTaskDefinitionKey());
            return true;
        }
        if (strategy == WorkflowEmptyAssigneeStrategy.AUTO_END) {
            saveRecord(task, WorkflowTaskAction.AUTO_END, "审批人为空，系统自动结束", variables);
            runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "审批人为空，系统自动结束");
            updateFormInstance(task.getProcessInstanceId(), variables, WorkflowInstanceStatus.ENDED);
            workflowEventPublisher.publishProcessEnded(task.getProcessInstanceId(), findFormInstance(task.getProcessInstanceId()),
                    variables, "审批人为空，系统自动结束");
            workflowBusinessApplyService.markTerminated(task.getProcessInstanceId(), "审批人为空，系统自动结束",
                    task.getId(), task.getTaskDefinitionKey());
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

    private WorkflowRenderConfigVO renderConfig(Task task, WorkflowFormInstance formInstance,
                                                Map<String, String> formPermissions) {
        return renderConfig(task.getProcessInstanceId(), task, formInstance, formPermissions);
    }

    private WorkflowRenderConfigVO renderConfig(String processInstanceId, Task task, WorkflowFormInstance formInstance,
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
        vo.setNodeExtension(config == null || config.getExtension() == null ? Map.of() : config.getExtension());
        vo.setFormPermissions(formPermissions == null ? Map.of() : formPermissions);
        vo.setBusinessPermissions(businessPermissions(variables, task == null ? null : task.getTaskDefinitionKey()));
        vo.setNodeActions(WorkflowNodeActionConfigResolver.resolve(config));
        return vo;
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
            return Require.fail(WorkflowCode.TASK_INVALID.getCode(), "当前任务引用的流程定义不存在，请清理测试脏数据后重试");
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
