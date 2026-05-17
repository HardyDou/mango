package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.workflow.api.WorkflowCode;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.enums.WorkflowApplyAction;
import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.query.WorkflowBusinessApplyPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessApplyCurrentTaskVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.core.entity.WorkflowBusinessApply;
import io.mango.workflow.core.entity.WorkflowBusinessApplyCurrentTask;
import io.mango.workflow.core.entity.WorkflowBusinessApplyStatusLog;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyCurrentTaskMapper;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyStatusLogMapper;
import io.mango.workflow.core.service.IWorkflowBusinessApplyService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
@RequiredArgsConstructor
public class WorkflowBusinessApplyServiceImpl implements IWorkflowBusinessApplyService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final DateTimeFormatter APPLY_CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final WorkflowBusinessApplyMapper applyMapper;
    private final WorkflowBusinessApplyCurrentTaskMapper currentTaskMapper;
    private final WorkflowBusinessApplyStatusLogMapper statusLogMapper;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<WorkflowBusinessApplyVO> create(CreateWorkflowBusinessApplyCommand command) {
        Require.notNull(command, WorkflowCode.APPLY_INVALID);
        Require.notBlank(command.getBusinessType(), WorkflowCode.APPLY_INVALID.getCode(), "业务类型不能为空");
        Require.notBlank(command.getBusinessKey(), WorkflowCode.APPLY_INVALID.getCode(), "业务主键不能为空");
        Require.notBlank(command.getApplyTitle(), WorkflowCode.APPLY_INVALID.getCode(), "申请标题不能为空");

        LocalDateTime now = LocalDateTime.now();
        WorkflowBusinessApply apply = new WorkflowBusinessApply();
        apply.setTenantId(currentTenantId());
        apply.setApplyCode(resolveApplyCode(command, now));
        apply.setBusinessType(command.getBusinessType().trim());
        apply.setBusinessKey(command.getBusinessKey().trim());
        apply.setApplyTitle(command.getApplyTitle().trim());
        apply.setApplySummary(trim(command.getApplySummary()));
        apply.setApplicantId(MangoContextHolder.userId());
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
        saveStatusLog(apply, null, WorkflowApplyStatus.DRAFT.name(), WorkflowApplyAction.CREATE, null, null, null);
        return R.ok(toVo(apply, List.of()));
    }

    @Override
    public R<PageResult<WorkflowBusinessApplyVO>> page(WorkflowBusinessApplyPageQuery query) {
        WorkflowBusinessApplyPageQuery resolved = query == null ? new WorkflowBusinessApplyPageQuery() : query;
        Page<WorkflowBusinessApply> page = new Page<>(resolved.getPage(), resolved.getSize());
        Page<WorkflowBusinessApply> result = applyMapper.selectPage(page, wrapper(resolved));
        List<WorkflowBusinessApplyVO> records = withCurrentTasks(result.getRecords()).stream()
                .map(entry -> toVo(entry.apply(), entry.tasks()))
                .toList();
        return R.ok(PageResult.of(records, result.getTotal(), resolved.getPage(), resolved.getSize()));
    }

    @Override
    public R<WorkflowBusinessApplyVO> detail(Long applyId) {
        Require.notNull(applyId, WorkflowCode.APPLY_INVALID.getCode(), "申请ID不能为空");
        WorkflowBusinessApply apply = applyMapper.selectById(applyId);
        Require.notNull(apply, WorkflowCode.APPLY_NOT_FOUND);
        return R.ok(toVo(apply, tasksByApplyId(apply.getId())));
    }

    @Override
    public R<WorkflowBusinessApplyVO> byProcessInstance(String processInstanceId) {
        Require.notBlank(processInstanceId, WorkflowCode.APPLY_INVALID.getCode(), "流程实例ID不能为空");
        WorkflowBusinessApply apply = applyByProcessInstanceId(processInstanceId);
        Require.notNull(apply, WorkflowCode.APPLY_NOT_FOUND);
        return R.ok(toVo(apply, tasksByApplyId(apply.getId())));
    }

    @Override
    public R<PageResult<WorkflowBusinessApplyVO>> history(String businessType, String businessKey, WorkflowBusinessApplyPageQuery query) {
        Require.notBlank(businessType, WorkflowCode.APPLY_INVALID.getCode(), "业务类型不能为空");
        Require.notBlank(businessKey, WorkflowCode.APPLY_INVALID.getCode(), "业务主键不能为空");
        WorkflowBusinessApplyPageQuery resolved = query == null ? new WorkflowBusinessApplyPageQuery() : query;
        resolved.setBusinessType(businessType);
        resolved.setBusinessKey(businessKey);
        resolved.setLatestOnly(Boolean.FALSE);
        return page(resolved);
    }

    @Override
    public R<WorkflowBusinessApplyProgressVO> latestProgress(String businessType, String businessKey) {
        Require.notBlank(businessType, WorkflowCode.APPLY_INVALID.getCode(), "业务类型不能为空");
        Require.notBlank(businessKey, WorkflowCode.APPLY_INVALID.getCode(), "业务主键不能为空");
        WorkflowBusinessApply apply = latestApply(businessType, businessKey);
        if (apply == null) {
            return R.ok(null);
        }
        return R.ok(toProgressVo(apply, tasksByApplyId(apply.getId())));
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
        LambdaQueryWrapper<WorkflowBusinessApply> wrapper = new LambdaQueryWrapper<WorkflowBusinessApply>()
                .in(WorkflowBusinessApply::getBusinessKey, keys)
                .eq(WorkflowBusinessApply::getLatestFlag, Boolean.TRUE)
                .orderByDesc(WorkflowBusinessApply::getCreatedAt);
        wrapper.eq(StringUtils.hasText(businessType), WorkflowBusinessApply::getBusinessType, trim(businessType));
        List<WorkflowBusinessApply> applies = applyMapper.selectList(wrapper);
        Map<Long, List<WorkflowBusinessApplyCurrentTask>> taskMap = tasksByApplyIds(applies.stream()
                .map(WorkflowBusinessApply::getId)
                .toList());
        Map<String, WorkflowBusinessApplyProgressVO> result = new LinkedHashMap<>();
        for (WorkflowBusinessApply apply : applies) {
            result.putIfAbsent(apply.getBusinessKey(), toProgressVo(apply, taskMap.getOrDefault(apply.getId(), List.of())));
        }
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
    public void markProcessStarted(Long applyId, Long processDefinitionId, String processDefinitionKey,
                                   String engineProcessDefinitionId, String processName, String processInstanceId) {
        if (applyId == null || !StringUtils.hasText(processInstanceId)) {
            return;
        }
        WorkflowBusinessApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String fromStatus = apply.getApplyStatus();
        apply.setProcessDefinitionId(processDefinitionId);
        apply.setProcessDefinitionKey(trim(processDefinitionKey));
        apply.setEngineProcessDefinitionId(trim(engineProcessDefinitionId));
        apply.setProcessInstanceId(processInstanceId);
        apply.setProcessName(trim(processName));
        apply.setApplyStatus(WorkflowApplyStatus.IN_APPROVAL.name());
        apply.setUpdatedBy(MangoContextHolder.userId());
        apply.setUpdatedTime(now);
        apply.setUpdatedAt(now);
        applyMapper.updateById(apply);
        saveStatusLog(apply, fromStatus, WorkflowApplyStatus.IN_APPROVAL.name(), WorkflowApplyAction.START_PROCESS, null, null, null);
        refreshCurrentTasks(processInstanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshCurrentTasks(String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId)) {
            return;
        }
        WorkflowBusinessApply apply = applyByProcessInstanceId(processInstanceId);
        if (apply == null) {
            return;
        }
        currentTaskMapper.delete(new LambdaQueryWrapper<WorkflowBusinessApplyCurrentTask>()
                .eq(WorkflowBusinessApplyCurrentTask::getApplyId, apply.getId()));
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .asc()
                .list();
        LocalDateTime now = LocalDateTime.now();
        for (Task task : tasks) {
            WorkflowBusinessApplyCurrentTask currentTask = new WorkflowBusinessApplyCurrentTask();
            currentTask.setTenantId(currentTenantId());
            currentTask.setApplyId(apply.getId());
            currentTask.setBusinessType(apply.getBusinessType());
            currentTask.setBusinessKey(apply.getBusinessKey());
            currentTask.setProcessInstanceId(processInstanceId);
            currentTask.setTaskId(task.getId());
            currentTask.setTaskDefinitionKey(task.getTaskDefinitionKey());
            currentTask.setTaskName(task.getName());
            currentTask.setAssigneeName(task.getAssignee());
            currentTask.setArrivedAt(task.getCreateTime() == null
                    ? now
                    : task.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            currentTask.setCreatedAt(now);
            currentTask.setUpdatedAt(now);
            currentTaskMapper.insert(currentTask);
        }
        updateCurrentTaskSummary(apply, tasks, now);
        if (!tasks.isEmpty() && WorkflowApplyStatus.SUBMITTED.name().equals(apply.getApplyStatus())) {
            updateStatus(apply, WorkflowApplyStatus.IN_APPROVAL, WorkflowApplyAction.TASK_CREATED, null, null, null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markApproved(String processInstanceId) {
        WorkflowBusinessApply apply = applyByProcessInstanceId(processInstanceId);
        if (apply == null) {
            return;
        }
        clearCurrentTasks(apply.getId());
        updateCurrentTaskSummary(apply, List.of(), LocalDateTime.now());
        updateStatus(apply, WorkflowApplyStatus.APPROVED, WorkflowApplyAction.COMPLETE, null, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRejected(String processInstanceId, String comment, String taskId, String taskDefinitionKey) {
        WorkflowBusinessApply apply = applyByProcessInstanceId(processInstanceId);
        if (apply == null) {
            return;
        }
        clearCurrentTasks(apply.getId());
        updateCurrentTaskSummary(apply, List.of(), LocalDateTime.now());
        updateStatus(apply, WorkflowApplyStatus.REJECTED, WorkflowApplyAction.REJECT, comment, taskId, taskDefinitionKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markTerminated(String processInstanceId, String comment, String taskId, String taskDefinitionKey) {
        WorkflowBusinessApply apply = applyByProcessInstanceId(processInstanceId);
        if (apply == null) {
            return;
        }
        clearCurrentTasks(apply.getId());
        updateCurrentTaskSummary(apply, List.of(), LocalDateTime.now());
        updateStatus(apply, WorkflowApplyStatus.TERMINATED, WorkflowApplyAction.TERMINATE, comment, taskId, taskDefinitionKey);
    }

    private LambdaQueryWrapper<WorkflowBusinessApply> wrapper(WorkflowBusinessApplyPageQuery query) {
        LambdaQueryWrapper<WorkflowBusinessApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getBusinessType()), WorkflowBusinessApply::getBusinessType, trim(query.getBusinessType()));
        wrapper.eq(StringUtils.hasText(query.getBusinessKey()), WorkflowBusinessApply::getBusinessKey, trim(query.getBusinessKey()));
        wrapper.eq(query.getApplicantId() != null, WorkflowBusinessApply::getApplicantId, query.getApplicantId());
        wrapper.eq(Boolean.TRUE.equals(query.getLatestOnly()), WorkflowBusinessApply::getLatestFlag, Boolean.TRUE);
        if (query.getStatuses() != null && !query.getStatuses().isEmpty()) {
            wrapper.in(WorkflowBusinessApply::getApplyStatus, query.getStatuses().stream().map(Enum::name).toList());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = "%" + query.getKeyword().trim() + "%";
            wrapper.and(item -> item.like(WorkflowBusinessApply::getApplyCode, keyword)
                    .or()
                    .like(WorkflowBusinessApply::getApplyTitle, keyword)
                    .or()
                    .like(WorkflowBusinessApply::getApplySummary, keyword));
        }
        if (query.getCurrentTaskDefinitionKeys() != null && !query.getCurrentTaskDefinitionKeys().isEmpty()) {
            wrapper.and(item -> {
                for (String key : cleanStrings(query.getCurrentTaskDefinitionKeys())) {
                    item.like(WorkflowBusinessApply::getCurrentTaskDefinitionKeys, key);
                }
            });
        }
        if (query.getCurrentAssigneeIds() != null && !query.getCurrentAssigneeIds().isEmpty()) {
            List<Long> applyIds = currentTaskMapper.selectList(new LambdaQueryWrapper<WorkflowBusinessApplyCurrentTask>()
                            .in(WorkflowBusinessApplyCurrentTask::getAssigneeId, query.getCurrentAssigneeIds()))
                    .stream()
                    .map(WorkflowBusinessApplyCurrentTask::getApplyId)
                    .distinct()
                    .toList();
            wrapper.in(!applyIds.isEmpty(), WorkflowBusinessApply::getId, applyIds);
            wrapper.eq(applyIds.isEmpty(), WorkflowBusinessApply::getId, -1L);
        }
        wrapper.ge(query.getStartedAtBegin() != null, WorkflowBusinessApply::getCreatedAt, query.getStartedAtBegin());
        wrapper.le(query.getStartedAtEnd() != null, WorkflowBusinessApply::getCreatedAt, query.getStartedAtEnd());
        wrapper.orderByDesc(WorkflowBusinessApply::getCreatedAt);
        return wrapper;
    }

    private WorkflowBusinessApply latestApply(String businessType, String businessKey) {
        return applyMapper.selectOne(new LambdaQueryWrapper<WorkflowBusinessApply>()
                .eq(WorkflowBusinessApply::getBusinessType, businessType.trim())
                .eq(WorkflowBusinessApply::getBusinessKey, businessKey.trim())
                .eq(WorkflowBusinessApply::getLatestFlag, Boolean.TRUE)
                .orderByDesc(WorkflowBusinessApply::getCreatedAt)
                .last("limit 1"));
    }

    private WorkflowBusinessApply applyByProcessInstanceId(String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId)) {
            return null;
        }
        return applyMapper.selectOne(new LambdaQueryWrapper<WorkflowBusinessApply>()
                .eq(WorkflowBusinessApply::getProcessInstanceId, processInstanceId)
                .last("limit 1"));
    }

    private void updateCurrentTaskSummary(WorkflowBusinessApply apply, List<Task> tasks, LocalDateTime now) {
        applyMapper.update(null, new LambdaUpdateWrapper<WorkflowBusinessApply>()
                .eq(WorkflowBusinessApply::getId, apply.getId())
                .set(WorkflowBusinessApply::getCurrentTaskNames, join(tasks.stream().map(Task::getName).toList()))
                .set(WorkflowBusinessApply::getCurrentTaskDefinitionKeys, join(tasks.stream().map(Task::getTaskDefinitionKey).toList()))
                .set(WorkflowBusinessApply::getCurrentAssigneeNames, join(tasks.stream().map(Task::getAssignee).toList()))
                .set(WorkflowBusinessApply::getUpdatedBy, MangoContextHolder.userId())
                .set(WorkflowBusinessApply::getUpdatedTime, now)
                .set(WorkflowBusinessApply::getUpdatedAt, now));
    }

    private void updateStatus(WorkflowBusinessApply apply, WorkflowApplyStatus status, WorkflowApplyAction action,
                              String comment, String taskId, String taskDefinitionKey) {
        String fromStatus = apply.getApplyStatus();
        if (status.name().equals(fromStatus)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        applyMapper.update(null, new LambdaUpdateWrapper<WorkflowBusinessApply>()
                .eq(WorkflowBusinessApply::getId, apply.getId())
                .set(WorkflowBusinessApply::getApplyStatus, status.name())
                .set(WorkflowBusinessApply::getUpdatedBy, MangoContextHolder.userId())
                .set(WorkflowBusinessApply::getUpdatedTime, now)
                .set(WorkflowBusinessApply::getUpdatedAt, now));
        apply.setApplyStatus(status.name());
        saveStatusLog(apply, fromStatus, status.name(), action, comment, taskId, taskDefinitionKey);
    }

    private void clearCurrentTasks(Long applyId) {
        currentTaskMapper.delete(new LambdaQueryWrapper<WorkflowBusinessApplyCurrentTask>()
                .eq(WorkflowBusinessApplyCurrentTask::getApplyId, applyId));
    }

    private void clearLatestFlag(String businessType, String businessKey) {
        applyMapper.update(null, new LambdaUpdateWrapper<WorkflowBusinessApply>()
                .eq(WorkflowBusinessApply::getBusinessType, businessType)
                .eq(WorkflowBusinessApply::getBusinessKey, businessKey)
                .eq(WorkflowBusinessApply::getLatestFlag, Boolean.TRUE)
                .set(WorkflowBusinessApply::getLatestFlag, Boolean.FALSE)
                .set(WorkflowBusinessApply::getUpdatedAt, LocalDateTime.now())
                .set(WorkflowBusinessApply::getUpdatedTime, LocalDateTime.now())
                .set(WorkflowBusinessApply::getUpdatedBy, MangoContextHolder.userId()));
    }

    private List<ApplyWithTasks> withCurrentTasks(List<WorkflowBusinessApply> applies) {
        Map<Long, List<WorkflowBusinessApplyCurrentTask>> taskMap = tasksByApplyIds(applies.stream()
                .map(WorkflowBusinessApply::getId)
                .toList());
        return applies.stream()
                .map(apply -> new ApplyWithTasks(apply, taskMap.getOrDefault(apply.getId(), List.of())))
                .toList();
    }

    private List<WorkflowBusinessApplyCurrentTask> tasksByApplyId(Long applyId) {
        if (applyId == null) {
            return List.of();
        }
        return currentTaskMapper.selectList(new LambdaQueryWrapper<WorkflowBusinessApplyCurrentTask>()
                .eq(WorkflowBusinessApplyCurrentTask::getApplyId, applyId)
                .orderByAsc(WorkflowBusinessApplyCurrentTask::getArrivedAt));
    }

    private Map<Long, List<WorkflowBusinessApplyCurrentTask>> tasksByApplyIds(List<Long> applyIds) {
        if (applyIds == null || applyIds.isEmpty()) {
            return Map.of();
        }
        return currentTaskMapper.selectList(new LambdaQueryWrapper<WorkflowBusinessApplyCurrentTask>()
                        .in(WorkflowBusinessApplyCurrentTask::getApplyId, applyIds)
                        .orderByAsc(WorkflowBusinessApplyCurrentTask::getArrivedAt))
                .stream()
                .collect(Collectors.groupingBy(WorkflowBusinessApplyCurrentTask::getApplyId, LinkedHashMap::new, Collectors.toList()));
    }

    private WorkflowBusinessApplyVO toVo(WorkflowBusinessApply apply, List<WorkflowBusinessApplyCurrentTask> tasks) {
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
        vo.setFormKey(apply.getFormKey());
        vo.setFormVersion(apply.getFormVersion());
        vo.setSnapshotRef(apply.getSnapshotRef());
        vo.setReapplyFromApplyId(apply.getReapplyFromApplyId());
        vo.setLatestFlag(apply.getLatestFlag());
        vo.setVariables(parseMap(apply.getVariablesJson()));
        vo.setExtension(parseMap(apply.getExtensionJson()));
        vo.setCurrentTasks(tasks.stream().map(this::toTaskVo).toList());
        vo.setCreatedAt(apply.getCreatedAt());
        vo.setUpdatedAt(apply.getUpdatedAt());
        return vo;
    }

    private WorkflowBusinessApplyProgressVO toProgressVo(WorkflowBusinessApply apply, List<WorkflowBusinessApplyCurrentTask> tasks) {
        WorkflowBusinessApplyProgressVO vo = new WorkflowBusinessApplyProgressVO();
        vo.setApplyId(apply.getId());
        vo.setApplyCode(apply.getApplyCode());
        vo.setBusinessType(apply.getBusinessType());
        vo.setBusinessKey(apply.getBusinessKey());
        vo.setApplyTitle(apply.getApplyTitle());
        vo.setProcessInstanceId(apply.getProcessInstanceId());
        vo.setProcessName(apply.getProcessName());
        WorkflowApplyStatus status = WorkflowApplyStatus.fromCode(apply.getApplyStatus());
        vo.setApplyStatus(status);
        vo.setApplyStatusName(status == null ? apply.getApplyStatus() : status.getLabel());
        vo.setCurrentTaskNames(apply.getCurrentTaskNames());
        vo.setCurrentTaskDefinitionKeys(apply.getCurrentTaskDefinitionKeys());
        vo.setCurrentAssigneeNames(apply.getCurrentAssigneeNames());
        vo.setCurrentTasks(tasks.stream().map(this::toTaskVo).toList());
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

    private WorkflowBusinessApplyCurrentTaskVO toTaskVo(WorkflowBusinessApplyCurrentTask task) {
        WorkflowBusinessApplyCurrentTaskVO vo = new WorkflowBusinessApplyCurrentTaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskDefinitionKey(task.getTaskDefinitionKey());
        vo.setTaskName(task.getTaskName());
        vo.setAssigneeId(task.getAssigneeId());
        vo.setAssigneeName(task.getAssigneeName());
        vo.setArrivedAt(task.getArrivedAt());
        return vo;
    }

    private void saveStatusLog(WorkflowBusinessApply apply, String fromStatus, String toStatus,
                               WorkflowApplyAction action, String comment, String taskId, String taskDefinitionKey) {
        WorkflowBusinessApplyStatusLog log = new WorkflowBusinessApplyStatusLog();
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

    private record ApplyWithTasks(WorkflowBusinessApply apply, List<WorkflowBusinessApplyCurrentTask> tasks) {
    }
}
