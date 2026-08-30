package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.command.WorkflowJsonRequest;
import io.mango.workflow.api.enums.WorkflowApplyAction;
import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowTaskClaimStatus;
import io.mango.workflow.api.query.WorkflowBusinessApplyPageQuery;
import io.mango.workflow.api.request.WorkflowBusinessApplyProgressBatchRequest;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressBatchVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyCurrentTaskVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplySummaryVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.api.vo.WorkflowJsonVO;
import io.mango.workflow.core.entity.WorkflowBusinessApplyEntity;
import io.mango.workflow.core.entity.WorkflowBusinessApplyCurrentTaskEntity;
import io.mango.workflow.core.entity.WorkflowBusinessApplyStatusLogEntity;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyCurrentTaskMapper;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyStatusLogMapper;
import io.mango.workflow.core.identity.WorkflowAssigneeIdentityService;
import io.mango.workflow.core.identity.WorkflowAssigneeIdentity;
import io.mango.workflow.core.model.WorkflowProcessStartedContext;
import io.mango.workflow.core.model.WorkflowTaskStatusContext;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 业务工作流申请中心服务实现。
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-managed collaborators are retained for the service lifetime"))
public class WorkflowBusinessApplyService implements IWorkflowBusinessApplyService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final DateTimeFormatter APPLY_CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final WorkflowBusinessApplyMapper applyMapper;
    private final WorkflowBusinessApplyCurrentTaskMapper currentTaskMapper;
    private final WorkflowBusinessApplyStatusLogMapper statusLogMapper;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<WorkflowBusinessApplyAccessChecker> accessCheckerProvider;
    private final WorkflowAssigneeIdentityService assigneeIdentityService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowBusinessApplyVO create(CreateWorkflowBusinessApplyCommand command) {
        Require.notNull(command, WorkflowCode.APPLY_INVALID);
        Require.notBlank(command.getBusinessType(), WorkflowCode.APPLY_INVALID, "业务类型不能为空");
        Require.notBlank(command.getBusinessKey(), WorkflowCode.APPLY_INVALID, "业务主键不能为空");
        Require.notBlank(command.getApplyTitle(), WorkflowCode.APPLY_INVALID, "申请标题不能为空");

        LocalDateTime now = LocalDateTime.now();
        WorkflowBusinessApplyEntity apply = new WorkflowBusinessApplyEntity();
        apply.setTenantId(currentTenantId());
        apply.setApplyCode(resolveApplyCode(command, now));
        apply.setBusinessType(command.getBusinessType().trim());
        apply.setBusinessKey(command.getBusinessKey().trim());
        apply.setApplyTitle(command.getApplyTitle().trim());
        apply.setApplySummary(trim(command.getApplySummary()));
        apply.setApplicantId(MangoContextHolder.userId());
        apply.setApplicantDeptId(command.getApplicantDeptId());
        apply.setApplicantName(currentUser());
        apply.setProcessDefinitionId(command.getProcessDefinitionId());
        apply.setProcessDefinitionKey(trim(command.getProcessDefinitionKey()));
        WorkflowApplyRenderMode renderMode = command.getRenderMode() == null
                ? WorkflowApplyRenderMode.DYNAMIC_FORM
                : command.getRenderMode();
        apply.setRenderMode(renderMode.name());
        apply.setApplyPageKey(trim(command.getApplyPageKey()));
        apply.setApprovePageKey(trim(command.getApprovePageKey()));
        apply.setFormKey(trim(command.getFormKey()));
        apply.setFormVersion(command.getFormVersion());
        apply.setFormJsonSnapshot(command.getFormJsonSnapshot());
        apply.setFormDataSnapshot(command.getFormDataSnapshot());
        apply.setSnapshotRef(trim(command.getSnapshotRef()));
        apply.setSnapshotDigest(trim(command.getSnapshotDigest()));
        apply.setVariablesJson(toJson(command.getVariables()));
        apply.setExtensionJson(toJson(command.getExtension()));
        apply.setReapplyFromApplyId(command.getReapplyFromApplyId());
        apply.setApplyStatus(WorkflowApplyStatus.DRAFT.name());
        apply.setLatestFlag(Boolean.TRUE);
        apply.setCreatedBy(MangoContextHolder.userId());
        apply.setCreatedTime(now);
        apply.setCreatedAt(now);
        apply.setUpdatedBy(MangoContextHolder.userId());
        apply.setUpdatedTime(now);
        apply.setUpdatedAt(now);

        clearLatestFlag(apply.getBusinessType(), apply.getBusinessKey());
        applyMapper.insert(apply);
        enrichApplyId(command, apply);
        saveStatusLog(apply, null, WorkflowApplyStatus.DRAFT.name(), WorkflowApplyAction.CREATE, null, null, null);
        return enrich(toVo(apply, List.of()));
    }

    private void enrichApplyId(CreateWorkflowBusinessApplyCommand command, WorkflowBusinessApplyEntity apply) {
        Map<String, Object> variables = command.getVariables() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(command.getVariables().toMap());
        variables.put("applyId", String.valueOf(apply.getId()));
        WorkflowJsonRequest enriched = WorkflowJsonRequest.of(variables);
        command.setVariables(enriched);
        apply.setVariablesJson(toJson(enriched));
        applyMapper.updateById(apply);
    }

    @Override
    public PageResult<WorkflowBusinessApplyVO> page(WorkflowBusinessApplyPageQuery query) {
        WorkflowBusinessApplyPageQuery resolved = query == null ? new WorkflowBusinessApplyPageQuery() : query;
        Page<WorkflowBusinessApplyEntity> page = new Page<>(resolved.getPage(), resolved.getSize());
        Page<WorkflowBusinessApplyEntity> result = applyMapper.selectPage(page, wrapper(resolved));
        List<WorkflowBusinessApplyVO> records = withCurrentTasks(result.getRecords()).stream()
                .map(entry -> toVo(entry.apply(), entry.tasks()))
                .toList();
        assigneeIdentityService.enrichBusinessApplies(records);
        return PageResult.of(records, result.getTotal(), resolved.getPage(), resolved.getSize());
    }

    @Override
    public WorkflowBusinessApplySummaryVO mySummary() {
        WorkflowBusinessApplySummaryVO summary = new WorkflowBusinessApplySummaryVO();
        Long userId = MangoContextHolder.userId();
        if (userId == null) {
            summary.setInReview(0L);
            summary.setCompleted(0L);
            summary.setRejected(0L);
            summary.setWithdrawn(0L);
            return summary;
        }
        // 小组件统计只暴露用户可见状态，不把底层 SUBMITTED/IN_APPROVAL 组合泄漏给前端。
        summary.setInReview(countMyApply(userId, WorkflowApplyStatus.SUBMITTED, WorkflowApplyStatus.IN_APPROVAL));
        summary.setCompleted(countMyApply(userId, WorkflowApplyStatus.APPROVED));
        summary.setRejected(countMyApply(userId, WorkflowApplyStatus.REJECTED));
        summary.setWithdrawn(countMyApply(userId, WorkflowApplyStatus.WITHDRAWN));
        return summary;
    }

    @Override
    public WorkflowBusinessApplyVO detail(Long applyId) {
        Require.notNull(applyId, WorkflowCode.APPLY_INVALID, "申请ID不能为空");
        WorkflowBusinessApplyEntity apply = applyMapper.selectById(applyId);
        Require.notNull(apply, WorkflowCode.APPLY_NOT_FOUND);
        checkAccess(apply);
        return enrich(toVo(apply, tasksByApplyId(apply.getId())));
    }

    @Override
    public WorkflowBusinessApplyVO byProcessInstance(String processInstanceId) {
        Require.notBlank(processInstanceId, WorkflowCode.APPLY_INVALID, "流程实例ID不能为空");
        WorkflowBusinessApplyEntity entity = applyByProcessInstanceId(processInstanceId);
        Require.notNull(entity, WorkflowCode.APPLY_NOT_FOUND);
        checkAccess(entity);
        return enrich(toVo(entity, tasksByApplyId(entity.getId())));
    }

    @Override
    public WorkflowBusinessApplyVO findByProcessInstance(String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId)) {
            return null;
        }
        WorkflowBusinessApplyEntity apply = applyByProcessInstanceId(processInstanceId);
        if (apply == null) {
            return null;
        }
        return enrich(toVo(apply, tasksByApplyId(apply.getId())));
    }

    @Override
    public WorkflowBusinessApplyVO lockWithdrawalTarget(Long applyId, String processInstanceId) {
        LambdaQueryWrapper<WorkflowBusinessApplyEntity> wrapper = new LambdaQueryWrapper<>();
        if (applyId != null) {
            wrapper.eq(WorkflowBusinessApplyEntity::getId, applyId);
        } else {
            wrapper.eq(WorkflowBusinessApplyEntity::getProcessInstanceId, trim(processInstanceId));
        }
        WorkflowBusinessApplyEntity apply = applyMapper.selectOne(wrapper.last("limit 1 for update"));
        return apply == null ? null : enrich(toVo(apply, tasksByApplyId(apply.getId())));
    }

    @Override
    public PageResult<WorkflowBusinessApplyVO> history(WorkflowBusinessApplyPageQuery query) {
        WorkflowBusinessApplyPageQuery resolved = query == null ? new WorkflowBusinessApplyPageQuery() : query;
        Require.notBlank(resolved.getBusinessType(), WorkflowCode.APPLY_INVALID, "业务类型不能为空");
        Require.notBlank(resolved.getBusinessKey(), WorkflowCode.APPLY_INVALID, "业务主键不能为空");
        resolved.setLatestOnly(Boolean.FALSE);
        List<WorkflowBusinessApplyEntity> applies = applyMapper.selectList(wrapper(resolved)).stream()
                .filter(this::isAllowed)
                .toList();
        long from = Math.max(0L, (resolved.getPage() - 1L) * resolved.getSize());
        long to = Math.min(applies.size(), from + resolved.getSize());
        List<WorkflowBusinessApplyVO> records = from >= to ? List.of()
                : withCurrentTasks(applies.subList((int) from, (int) to)).stream()
                .map(entry -> toVo(entry.apply(), entry.tasks()))
                .toList();
        assigneeIdentityService.enrichBusinessApplies(records);
        return PageResult.of(records, applies.size(), resolved.getPage(), resolved.getSize());
    }

    @Override
    public WorkflowBusinessApplyProgressVO latestProgress(String businessType, String businessKey) {
        Require.notBlank(businessType, WorkflowCode.APPLY_INVALID, "业务类型不能为空");
        Require.notBlank(businessKey, WorkflowCode.APPLY_INVALID, "业务主键不能为空");
        WorkflowBusinessApplyEntity apply = latestApply(businessType, businessKey);
        if (apply == null) {
            return null;
        }
        checkAccess(apply);
        WorkflowBusinessApplyProgressVO progress = toProgressVo(apply, tasksByApplyId(apply.getId()));
        assigneeIdentityService.enrichProgresses(List.of(progress));
        return progress;
    }

    @Override
    public Map<String, WorkflowBusinessApplyProgressVO> latestProgress(String businessType, Collection<String> businessKeys) {
        if (businessKeys == null || businessKeys.isEmpty()) {
            return Map.of();
        }
        List<String> keys = cleanStrings(businessKeys);
        if (keys.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<WorkflowBusinessApplyEntity> wrapper = new LambdaQueryWrapper<WorkflowBusinessApplyEntity>()
                .in(WorkflowBusinessApplyEntity::getBusinessKey, keys)
                .eq(WorkflowBusinessApplyEntity::getLatestFlag, Boolean.TRUE)
                .orderByDesc(WorkflowBusinessApplyEntity::getCreatedAt);
        wrapper.eq(StringUtils.hasText(businessType), WorkflowBusinessApplyEntity::getBusinessType, trim(businessType));
        List<WorkflowBusinessApplyEntity> applies = applyMapper.selectList(wrapper);
        Map<Long, List<WorkflowBusinessApplyCurrentTaskEntity>> taskMap = tasksByApplyIds(applies.stream()
                .map(WorkflowBusinessApplyEntity::getId)
                .toList());
        Map<String, WorkflowBusinessApplyProgressVO> result = new LinkedHashMap<>();
        for (WorkflowBusinessApplyEntity apply : applies) {
            if (!isAllowed(apply)) {
                continue;
            }
            result.putIfAbsent(apply.getBusinessKey(), toProgressVo(apply, taskMap.getOrDefault(apply.getId(), List.of())));
        }
        assigneeIdentityService.enrichProgresses(result.values());
        return result;
    }

    @Override
    public WorkflowBusinessApplyProgressBatchVO latestProgressBatch(
            WorkflowBusinessApplyProgressBatchRequest request) {
        Require.notNull(request, WorkflowCode.APPLY_INVALID);
        WorkflowBusinessApplyProgressBatchVO result = new WorkflowBusinessApplyProgressBatchVO();
        result.setRecords(List.copyOf(latestProgress(
                request.getBusinessType(), request.getBusinessKeys()).values()));
        return result;
    }

    @Override
    public List<WorkflowBusinessApplyVO> latestByBusinessKeys(String businessType, Collection<String> businessKeys) {
        return latestProgress(businessType, businessKeys).values().stream()
                .map(this::fromProgress)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markProcessStarted(WorkflowProcessStartedContext context) {
        if (context == null || context.applyId() == null || !StringUtils.hasText(context.processInstanceId())) {
            return;
        }
        WorkflowBusinessApplyEntity apply = applyMapper.selectById(context.applyId());
        if (apply == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String fromStatus = apply.getApplyStatus();
        apply.setProcessDefinitionId(context.processDefinitionId());
        apply.setProcessDefinitionKey(trim(context.processDefinitionKey()));
        apply.setEngineProcessDefinitionId(trim(context.engineProcessDefinitionId()));
        apply.setProcessInstanceId(context.processInstanceId());
        apply.setProcessName(trim(context.processName()));
        apply.setApplyStatus(WorkflowApplyStatus.IN_APPROVAL.name());
        apply.setUpdatedBy(MangoContextHolder.userId());
        apply.setUpdatedTime(now);
        apply.setUpdatedAt(now);
        applyMapper.updateById(apply);
        saveStatusLog(apply, fromStatus, WorkflowApplyStatus.IN_APPROVAL.name(), WorkflowApplyAction.START_PROCESS, null, null, null);
        refreshCurrentTasks(context.processInstanceId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshCurrentTasks(String processInstanceId) {
        refreshCurrentTasksAndReturn(processInstanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowBusinessApplyVO refreshCurrentTasksAndReturn(String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId)) {
            return null;
        }
        WorkflowBusinessApplyEntity apply = applyByProcessInstanceId(processInstanceId);
        if (apply == null) {
            return null;
        }
        currentTaskMapper.delete(new LambdaQueryWrapper<WorkflowBusinessApplyCurrentTaskEntity>()
                .eq(WorkflowBusinessApplyCurrentTaskEntity::getApplyId, apply.getId()));
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .asc()
                .list();
        Map<String, WorkflowAssigneeIdentity> identities = assigneeIdentityService.resolve(tasks.stream()
                .map(Task::getAssignee)
                .toList());
        List<WorkflowBusinessApplyCurrentTaskEntity> currentTasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Task task : tasks) {
            WorkflowBusinessApplyCurrentTaskEntity currentTask = new WorkflowBusinessApplyCurrentTaskEntity();
            currentTask.setTenantId(currentTenantId());
            currentTask.setApplyId(apply.getId());
            currentTask.setBusinessType(apply.getBusinessType());
            currentTask.setBusinessKey(apply.getBusinessKey());
            currentTask.setProcessInstanceId(processInstanceId);
            currentTask.setTaskId(task.getId());
            currentTask.setTaskDefinitionKey(task.getTaskDefinitionKey());
            currentTask.setTaskName(task.getName());
            String assigneeName = task.getAssignee();
            WorkflowAssigneeIdentity identity = StringUtils.hasText(assigneeName)
                    ? identities.get(assigneeName.trim())
                    : null;
            currentTask.setAssigneeId(identity == null ? null : identity.userId());
            currentTask.setAssigneeName(assigneeName);
            TaskCandidates candidates = candidates(task);
            WorkflowTaskClaimStatus claimStatus = currentClaimStatus(task, candidates);
            currentTask.setClaimStatus(claimStatus.name());
            currentTask.setCandidateUsers(join(candidates.users()));
            currentTask.setCandidateGroups(join(candidates.groups()));
            currentTask.setArrivedAt(task.getCreateTime() == null
                    ? now
                    : task.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            currentTask.setCreatedAt(now);
            currentTask.setUpdatedAt(now);
            currentTaskMapper.insert(currentTask);
            currentTasks.add(currentTask);
        }
        updateCurrentTaskSummary(apply, tasks, now);
        if (!tasks.isEmpty() && WorkflowApplyStatus.SUBMITTED.name().equals(apply.getApplyStatus())) {
            updateStatus(apply, WorkflowApplyStatus.IN_APPROVAL, WorkflowApplyAction.TASK_CREATED, null, null, null);
        }
        WorkflowBusinessApplyEntity refreshedApply = applyByProcessInstanceId(processInstanceId);
        if (refreshedApply == null) {
            return null;
        }
        WorkflowBusinessApplyVO result = toVo(refreshedApply, currentTasks);
        assigneeIdentityService.enrichCurrentTasks(result.getCurrentTasks(), identities);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markApproved(String processInstanceId) {
        WorkflowBusinessApplyEntity apply = applyByProcessInstanceId(processInstanceId);
        if (apply == null) {
            return;
        }
        clearCurrentTasks(apply.getId());
        updateCurrentTaskSummary(apply, List.of(), LocalDateTime.now());
        updateStatus(apply, WorkflowApplyStatus.APPROVED, WorkflowApplyAction.COMPLETE, null, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRejected(WorkflowTaskStatusContext context) {
        WorkflowBusinessApplyEntity apply = context == null ? null : applyByProcessInstanceId(context.processInstanceId());
        if (apply == null) {
            return;
        }
        clearCurrentTasks(apply.getId());
        updateCurrentTaskSummary(apply, List.of(), LocalDateTime.now());
        updateStatus(apply, WorkflowApplyStatus.REJECTED, WorkflowApplyAction.REJECT,
                context.comment(), context.taskId(), context.taskDefinitionKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markTerminated(WorkflowTaskStatusContext context) {
        WorkflowBusinessApplyEntity apply = context == null ? null : applyByProcessInstanceId(context.processInstanceId());
        if (apply == null) {
            return;
        }
        clearCurrentTasks(apply.getId());
        updateCurrentTaskSummary(apply, List.of(), LocalDateTime.now());
        updateStatus(apply, WorkflowApplyStatus.TERMINATED, WorkflowApplyAction.TERMINATE,
                context.comment(), context.taskId(), context.taskDefinitionKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowBusinessApplyVO markWithdrawn(String processInstanceId, String reason) {
        WorkflowBusinessApplyEntity apply = applyByProcessInstanceId(processInstanceId);
        Require.notNull(apply, WorkflowCode.APPLY_NOT_FOUND);
        clearCurrentTasks(apply.getId());
        updateCurrentTaskSummary(apply, List.of(), LocalDateTime.now());
        updateStatus(apply, WorkflowApplyStatus.WITHDRAWN, WorkflowApplyAction.WITHDRAW,
                trim(reason), null, null);
        return findByProcessInstance(processInstanceId);
    }

    private LambdaQueryWrapper<WorkflowBusinessApplyEntity> wrapper(WorkflowBusinessApplyPageQuery query) {
        LambdaQueryWrapper<WorkflowBusinessApplyEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getBusinessType()), WorkflowBusinessApplyEntity::getBusinessType, trim(query.getBusinessType()));
        wrapper.eq(StringUtils.hasText(query.getBusinessKey()), WorkflowBusinessApplyEntity::getBusinessKey, trim(query.getBusinessKey()));
        wrapper.eq(query.getApplicantId() != null, WorkflowBusinessApplyEntity::getApplicantId, query.getApplicantId());
        wrapper.eq(Boolean.TRUE.equals(query.getLatestOnly()), WorkflowBusinessApplyEntity::getLatestFlag, Boolean.TRUE);
        if (query.getStatuses() != null && !query.getStatuses().isEmpty()) {
            wrapper.in(WorkflowBusinessApplyEntity::getApplyStatus, query.getStatuses().stream().map(Enum::name).toList());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = "%" + query.getKeyword().trim() + "%";
            wrapper.and(item -> item.like(WorkflowBusinessApplyEntity::getApplyCode, keyword)
                    .or()
                    .like(WorkflowBusinessApplyEntity::getApplyTitle, keyword)
                    .or()
                    .like(WorkflowBusinessApplyEntity::getApplySummary, keyword));
        }
        if (query.getCurrentTaskDefinitionKeys() != null && !query.getCurrentTaskDefinitionKeys().isEmpty()) {
            List<String> taskDefinitionKeys = cleanStrings(query.getCurrentTaskDefinitionKeys());
            List<Long> applyIds = taskDefinitionKeys.isEmpty()
                    ? List.of()
                    : currentTaskMapper.selectList(new LambdaQueryWrapper<WorkflowBusinessApplyCurrentTaskEntity>()
                                    .in(WorkflowBusinessApplyCurrentTaskEntity::getTaskDefinitionKey, taskDefinitionKeys))
                            .stream()
                            .map(WorkflowBusinessApplyCurrentTaskEntity::getApplyId)
                            .distinct()
                            .toList();
            wrapper.in(!applyIds.isEmpty(), WorkflowBusinessApplyEntity::getId, applyIds);
            wrapper.eq(applyIds.isEmpty(), WorkflowBusinessApplyEntity::getId, -1L);
        }
        if (query.getCurrentAssigneeIds() != null && !query.getCurrentAssigneeIds().isEmpty()) {
            List<Long> applyIds = currentTaskMapper.selectList(new LambdaQueryWrapper<WorkflowBusinessApplyCurrentTaskEntity>()
                            .in(WorkflowBusinessApplyCurrentTaskEntity::getAssigneeId, query.getCurrentAssigneeIds()))
                    .stream()
                    .map(WorkflowBusinessApplyCurrentTaskEntity::getApplyId)
                    .distinct()
                    .toList();
            wrapper.in(!applyIds.isEmpty(), WorkflowBusinessApplyEntity::getId, applyIds);
            wrapper.eq(applyIds.isEmpty(), WorkflowBusinessApplyEntity::getId, -1L);
        }
        wrapper.ge(query.getStartedAtBegin() != null, WorkflowBusinessApplyEntity::getCreatedAt, query.getStartedAtBegin());
        wrapper.le(query.getStartedAtEnd() != null, WorkflowBusinessApplyEntity::getCreatedAt, query.getStartedAtEnd());
        wrapper.orderByDesc(WorkflowBusinessApplyEntity::getCreatedAt);
        return wrapper;
    }

    private WorkflowBusinessApplyEntity latestApply(String businessType, String businessKey) {
        return applyMapper.selectOne(new LambdaQueryWrapper<WorkflowBusinessApplyEntity>()
                .eq(WorkflowBusinessApplyEntity::getBusinessType, businessType.trim())
                .eq(WorkflowBusinessApplyEntity::getBusinessKey, businessKey.trim())
                .eq(WorkflowBusinessApplyEntity::getLatestFlag, Boolean.TRUE)
                .orderByDesc(WorkflowBusinessApplyEntity::getCreatedAt)
                .last("limit 1"));
    }

    private Long countMyApply(Long userId, WorkflowApplyStatus... statuses) {
        return applyMapper.selectCount(new LambdaQueryWrapper<WorkflowBusinessApplyEntity>()
                .eq(WorkflowBusinessApplyEntity::getApplicantId, userId)
                .in(WorkflowBusinessApplyEntity::getApplyStatus, Arrays.stream(statuses).map(Enum::name).toList()));
    }

    private WorkflowBusinessApplyEntity applyByProcessInstanceId(String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId)) {
            return null;
        }
        return applyMapper.selectOne(new LambdaQueryWrapper<WorkflowBusinessApplyEntity>()
                .eq(WorkflowBusinessApplyEntity::getProcessInstanceId, processInstanceId)
                .last("limit 1"));
    }

    private void checkAccess(WorkflowBusinessApplyEntity apply) {
        WorkflowBusinessApplyAccessChecker checker = accessCheckerProvider.getIfAvailable();
        if (checker != null) {
            checker.check(apply);
        }
    }

    private boolean isAllowed(WorkflowBusinessApplyEntity apply) {
        WorkflowBusinessApplyAccessChecker checker = accessCheckerProvider.getIfAvailable();
        return checker == null || checker.isAllowed(apply);
    }

    private void updateCurrentTaskSummary(WorkflowBusinessApplyEntity apply, List<Task> tasks, LocalDateTime now) {
        applyMapper.update(null, new LambdaUpdateWrapper<WorkflowBusinessApplyEntity>()
                .eq(WorkflowBusinessApplyEntity::getId, apply.getId())
                .set(WorkflowBusinessApplyEntity::getCurrentTaskNames, join(tasks.stream().map(Task::getName).toList()))
                .set(WorkflowBusinessApplyEntity::getCurrentTaskDefinitionKeys, join(tasks.stream().map(Task::getTaskDefinitionKey).toList()))
                .set(WorkflowBusinessApplyEntity::getCurrentAssigneeNames, join(tasks.stream()
                        .map(this::currentAssigneeDisplay)
                        .toList()))
                .set(WorkflowBusinessApplyEntity::getUpdatedBy, MangoContextHolder.userId())
                .set(WorkflowBusinessApplyEntity::getUpdatedTime, now)
                .set(WorkflowBusinessApplyEntity::getUpdatedAt, now));
    }

    private void updateStatus(WorkflowBusinessApplyEntity apply, WorkflowApplyStatus status, WorkflowApplyAction action,
                              String comment, String taskId, String taskDefinitionKey) {
        String fromStatus = apply.getApplyStatus();
        if (status.name().equals(fromStatus)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        applyMapper.update(null, new LambdaUpdateWrapper<WorkflowBusinessApplyEntity>()
                .eq(WorkflowBusinessApplyEntity::getId, apply.getId())
                .set(WorkflowBusinessApplyEntity::getApplyStatus, status.name())
                .set(WorkflowBusinessApplyEntity::getUpdatedBy, MangoContextHolder.userId())
                .set(WorkflowBusinessApplyEntity::getUpdatedTime, now)
                .set(WorkflowBusinessApplyEntity::getUpdatedAt, now));
        apply.setApplyStatus(status.name());
        saveStatusLog(apply, fromStatus, status.name(), action, comment, taskId, taskDefinitionKey);
    }

    private void clearCurrentTasks(Long applyId) {
        currentTaskMapper.delete(new LambdaQueryWrapper<WorkflowBusinessApplyCurrentTaskEntity>()
                .eq(WorkflowBusinessApplyCurrentTaskEntity::getApplyId, applyId));
    }

    private void clearLatestFlag(String businessType, String businessKey) {
        applyMapper.update(null, new LambdaUpdateWrapper<WorkflowBusinessApplyEntity>()
                .eq(WorkflowBusinessApplyEntity::getBusinessType, businessType)
                .eq(WorkflowBusinessApplyEntity::getBusinessKey, businessKey)
                .eq(WorkflowBusinessApplyEntity::getLatestFlag, Boolean.TRUE)
                .set(WorkflowBusinessApplyEntity::getLatestFlag, Boolean.FALSE)
                .set(WorkflowBusinessApplyEntity::getUpdatedAt, LocalDateTime.now())
                .set(WorkflowBusinessApplyEntity::getUpdatedTime, LocalDateTime.now())
                .set(WorkflowBusinessApplyEntity::getUpdatedBy, MangoContextHolder.userId()));
    }

    private List<ApplyWithTasks> withCurrentTasks(List<WorkflowBusinessApplyEntity> applies) {
        Map<Long, List<WorkflowBusinessApplyCurrentTaskEntity>> taskMap = tasksByApplyIds(applies.stream()
                .map(WorkflowBusinessApplyEntity::getId)
                .toList());
        return applies.stream()
                .map(apply -> new ApplyWithTasks(apply, taskMap.getOrDefault(apply.getId(), List.of())))
                .toList();
    }

    private List<WorkflowBusinessApplyCurrentTaskEntity> tasksByApplyId(Long applyId) {
        if (applyId == null) {
            return List.of();
        }
        return currentTaskMapper.selectList(new LambdaQueryWrapper<WorkflowBusinessApplyCurrentTaskEntity>()
                .eq(WorkflowBusinessApplyCurrentTaskEntity::getApplyId, applyId)
                .orderByAsc(WorkflowBusinessApplyCurrentTaskEntity::getArrivedAt));
    }

    private Map<Long, List<WorkflowBusinessApplyCurrentTaskEntity>> tasksByApplyIds(List<Long> applyIds) {
        if (applyIds == null || applyIds.isEmpty()) {
            return Map.of();
        }
        return currentTaskMapper.selectList(new LambdaQueryWrapper<WorkflowBusinessApplyCurrentTaskEntity>()
                        .in(WorkflowBusinessApplyCurrentTaskEntity::getApplyId, applyIds)
                        .orderByAsc(WorkflowBusinessApplyCurrentTaskEntity::getArrivedAt))
                .stream()
                .collect(Collectors.groupingBy(WorkflowBusinessApplyCurrentTaskEntity::getApplyId, LinkedHashMap::new, Collectors.toList()));
    }

    private WorkflowBusinessApplyVO toVo(WorkflowBusinessApplyEntity apply, List<WorkflowBusinessApplyCurrentTaskEntity> tasks) {
        WorkflowBusinessApplyVO vo = new WorkflowBusinessApplyVO();
        vo.setId(apply.getId());
        vo.setApplyCode(apply.getApplyCode());
        vo.setBusinessType(apply.getBusinessType());
        vo.setBusinessKey(apply.getBusinessKey());
        vo.setApplyTitle(apply.getApplyTitle());
        vo.setApplySummary(apply.getApplySummary());
        vo.setApplicantId(apply.getApplicantId());
        vo.setApplicantName(apply.getApplicantName());
        vo.setProcessDefinitionId(apply.getProcessDefinitionId());
        vo.setProcessDefinitionKey(apply.getProcessDefinitionKey());
        vo.setEngineProcessDefinitionId(apply.getEngineProcessDefinitionId());
        vo.setProcessInstanceId(apply.getProcessInstanceId());
        vo.setProcessName(apply.getProcessName());
        WorkflowApplyStatus status = WorkflowApplyStatus.fromCode(apply.getApplyStatus());
        vo.setApplyStatus(status);
        vo.setApplyStatusName(status == null ? apply.getApplyStatus() : status.getLabel());
        vo.setCurrentTaskNames(apply.getCurrentTaskNames());
        vo.setCurrentTaskDefinitionKeys(apply.getCurrentTaskDefinitionKeys());
        vo.setCurrentAssigneeNames(apply.getCurrentAssigneeNames());
        vo.setRenderMode(WorkflowApplyRenderMode.fromCode(apply.getRenderMode()));
        vo.setApplyPageKey(apply.getApplyPageKey());
        vo.setApprovePageKey(apply.getApprovePageKey());
        vo.setViewPath(customFormViewPath(apply.getFormJsonSnapshot()));
        vo.setFormKey(apply.getFormKey());
        vo.setFormVersion(apply.getFormVersion());
        vo.setSnapshotRef(apply.getSnapshotRef());
        vo.setReapplyFromApplyId(apply.getReapplyFromApplyId());
        vo.setLatestFlag(apply.getLatestFlag());
        vo.setVariables(WorkflowJsonVO.of(parseMap(apply.getVariablesJson())));
        vo.setExtension(WorkflowJsonVO.of(parseMap(apply.getExtensionJson())));
        vo.setCurrentTasks(tasks.stream().map(this::toTaskVo).toList());
        vo.setCreatedAt(apply.getCreatedAt());
        vo.setUpdatedAt(apply.getUpdatedAt());
        return vo;
    }

    private WorkflowBusinessApplyVO enrich(WorkflowBusinessApplyVO apply) {
        assigneeIdentityService.enrichBusinessApplies(List.of(apply));
        return apply;
    }

    private String customFormViewPath(String formJson) {
        Object customConfig = parseMap(formJson).get("customConfig");
        if (!(customConfig instanceof Map<?, ?> config)) {
            return null;
        }
        Object viewPath = config.get("viewPath");
        return viewPath == null ? null : trim(String.valueOf(viewPath));
    }

    private WorkflowBusinessApplyProgressVO toProgressVo(WorkflowBusinessApplyEntity apply, List<WorkflowBusinessApplyCurrentTaskEntity> tasks) {
        WorkflowBusinessApplyProgressVO vo = new WorkflowBusinessApplyProgressVO();
        vo.setApplyId(apply.getId());
        vo.setApplyCode(apply.getApplyCode());
        vo.setBusinessType(apply.getBusinessType());
        vo.setBusinessKey(apply.getBusinessKey());
        vo.setApplyTitle(apply.getApplyTitle());
        vo.setProcessInstanceId(apply.getProcessInstanceId());
        vo.setProcessName(apply.getProcessName());
        vo.setProcessDefinitionKey(apply.getProcessDefinitionKey());
        WorkflowApplyStatus status = WorkflowApplyStatus.fromCode(apply.getApplyStatus());
        vo.setApplyStatus(status);
        vo.setApplyStatusName(status == null ? apply.getApplyStatus() : status.getLabel());
        vo.setProcessStatus(status);
        vo.setProcessStatusName(status == null ? apply.getApplyStatus() : status.getLabel());
        vo.setCurrentTaskNames(apply.getCurrentTaskNames());
        vo.setCurrentTaskDefinitionKeys(apply.getCurrentTaskDefinitionKeys());
        vo.setCurrentAssigneeNames(apply.getCurrentAssigneeNames());
        List<WorkflowBusinessApplyCurrentTaskVO> currentTasks = tasks.stream().map(this::toTaskVo).toList();
        vo.setCurrentTasks(currentTasks);
        fillFirstTask(vo, currentTasks);
        vo.setStartedAt(apply.getCreatedAt());
        vo.setEndedAt(isEnded(status) ? apply.getUpdatedAt() : null);
        vo.setCreatedAt(apply.getCreatedAt());
        vo.setUpdatedAt(apply.getUpdatedAt());
        return vo;
    }

    private WorkflowBusinessApplyVO fromProgress(WorkflowBusinessApplyProgressVO progress) {
        WorkflowBusinessApplyVO vo = new WorkflowBusinessApplyVO();
        vo.setId(progress.getApplyId());
        vo.setApplyCode(progress.getApplyCode());
        vo.setBusinessType(progress.getBusinessType());
        vo.setBusinessKey(progress.getBusinessKey());
        vo.setApplyTitle(progress.getApplyTitle());
        vo.setProcessInstanceId(progress.getProcessInstanceId());
        vo.setProcessName(progress.getProcessName());
        vo.setApplyStatus(progress.getApplyStatus());
        vo.setApplyStatusName(progress.getApplyStatusName());
        vo.setCurrentTaskNames(progress.getCurrentTaskNames());
        vo.setCurrentTaskDefinitionKeys(progress.getCurrentTaskDefinitionKeys());
        vo.setCurrentAssigneeNames(progress.getCurrentAssigneeNames());
        vo.setCurrentTasks(progress.getCurrentTasks());
        vo.setCreatedAt(progress.getCreatedAt());
        vo.setUpdatedAt(progress.getUpdatedAt());
        return vo;
    }

    private WorkflowBusinessApplyCurrentTaskVO toTaskVo(WorkflowBusinessApplyCurrentTaskEntity task) {
        WorkflowBusinessApplyCurrentTaskVO vo = new WorkflowBusinessApplyCurrentTaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskDefinitionKey(task.getTaskDefinitionKey());
        vo.setTaskName(task.getTaskName());
        vo.setAssigneeId(task.getAssigneeId());
        vo.setAssigneeName(task.getAssigneeName());
        vo.setClaimStatus(claimStatusOf(task.getClaimStatus()));
        vo.setCandidateUsers(split(task.getCandidateUsers()));
        vo.setCandidateGroups(split(task.getCandidateGroups()));
        vo.setArrivedAt(task.getArrivedAt());
        return vo;
    }

    private void fillFirstTask(WorkflowBusinessApplyProgressVO vo, List<WorkflowBusinessApplyCurrentTaskVO> currentTasks) {
        if (currentTasks == null || currentTasks.isEmpty()) {
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
        vo.setAssigneeDisplayName(first.getAssigneeDisplayName());
        vo.setClaimStatus(first.getClaimStatus());
        vo.setCandidateUsers(first.getCandidateUsers());
        vo.setCandidateGroups(first.getCandidateGroups());
    }

    private WorkflowTaskClaimStatus currentClaimStatus(Task task, TaskCandidates candidates) {
        if (StringUtils.hasText(task.getAssignee())) {
            return WorkflowTaskClaimStatus.ASSIGNED;
        }
        if (!candidates.users().isEmpty() || !candidates.groups().isEmpty()) {
            return WorkflowTaskClaimStatus.UNCLAIMED;
        }
        return WorkflowTaskClaimStatus.UNCLAIMED;
    }

    private String currentAssigneeDisplay(Task task) {
        if (StringUtils.hasText(task.getAssignee())) {
            return task.getAssignee().trim();
        }
        TaskCandidates candidates = candidates(task);
        if (!candidates.users().isEmpty() || !candidates.groups().isEmpty()) {
            return WorkflowTaskClaimStatus.UNCLAIMED.getLabel();
        }
        return null;
    }

    private TaskCandidates candidates(Task task) {
        if (task == null || !StringUtils.hasText(task.getId())) {
            return new TaskCandidates(List.of(), List.of());
        }
        List<IdentityLink> links = taskService.getIdentityLinksForTask(task.getId());
        List<String> users = links.stream()
                .map(IdentityLink::getUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        List<String> groups = links.stream()
                .map(IdentityLink::getGroupId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        return new TaskCandidates(users, groups);
    }

    private boolean isEnded(WorkflowApplyStatus status) {
        return status == WorkflowApplyStatus.APPROVED
                || status == WorkflowApplyStatus.REJECTED
                || status == WorkflowApplyStatus.WITHDRAWN
                || status == WorkflowApplyStatus.CANCELED
                || status == WorkflowApplyStatus.TERMINATED;
    }

    private WorkflowTaskClaimStatus claimStatusOf(String claimStatus) {
        if (!StringUtils.hasText(claimStatus)) {
            return WorkflowTaskClaimStatus.NONE;
        }
        try {
            return WorkflowTaskClaimStatus.valueOf(claimStatus.trim());
        } catch (IllegalArgumentException ex) {
            return WorkflowTaskClaimStatus.NONE;
        }
    }

    private void saveStatusLog(WorkflowBusinessApplyEntity apply, String fromStatus, String toStatus,
                               WorkflowApplyAction action, String comment, String taskId, String taskDefinitionKey) {
        WorkflowBusinessApplyStatusLogEntity log = new WorkflowBusinessApplyStatusLogEntity();
        log.setTenantId(currentTenantId());
        log.setApplyId(apply.getId());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAction(action.name());
        log.setActionName(action.getLabel());
        log.setOperatorId(MangoContextHolder.userId());
        log.setOperatorName(currentUser());
        log.setComment(comment);
        log.setTaskId(taskId);
        log.setTaskDefinitionKey(taskDefinitionKey);
        log.setProcessInstanceId(apply.getProcessInstanceId());
        log.setCreatedAt(LocalDateTime.now());
        statusLogMapper.insert(log);
    }

    private String join(Collection<String> values) {
        return cleanStrings(values).isEmpty() ? null : String.join(",", cleanStrings(values));
    }

    private String resolveApplyCode(CreateWorkflowBusinessApplyCommand command, LocalDateTime now) {
        if (StringUtils.hasText(command.getApplyCode())) {
            return command.getApplyCode().trim();
        }
        String type = command.getBusinessType().trim().replaceAll("[^A-Za-z0-9]", "");
        String prefix = type.length() > 12 ? type.substring(0, 12) : type;
        return prefix.toUpperCase() + "-" + APPLY_CODE_TIME.format(now) + "-" + Math.abs(Objects.hash(command.getBusinessKey(), now));
    }

    private List<String> cleanStrings(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                result.add(value.trim());
            }
        }
        return List.copyOf(result);
    }

    private List<String> split(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return cleanStrings(Arrays.asList(value.split(",")));
    }

    private Map<String, Object> parseMap(String value) {
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new HashMap<>() : value);
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

    private record ApplyWithTasks(WorkflowBusinessApplyEntity apply, List<WorkflowBusinessApplyCurrentTaskEntity> tasks) {
    }

    private record TaskCandidates(List<String> users, List<String> groups) {
    }
}
