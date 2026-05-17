package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.workflow.api.WorkflowBusinessProcessApi;
import io.mango.workflow.api.WorkflowCode;
import io.mango.workflow.api.command.CreateWorkflowBusinessApplyCommand;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.enums.WorkflowDefinitionStatus;
import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.mango.workflow.api.enums.WorkflowInstanceStatus;
import io.mango.workflow.api.enums.WorkflowTaskAction;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessProcessVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.api.vo.WorkflowProcessDetailVO;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.entity.WorkflowFormInstance;
import io.mango.workflow.core.entity.WorkflowTaskRecord;
import io.mango.workflow.core.event.WorkflowEventPublisher;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
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
public class WorkflowProcessServiceImpl implements IWorkflowProcessService, WorkflowBusinessProcessApi {

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
    public R<WorkflowProcessInstanceVO> start(StartWorkflowProcessCommand command) {
        Require.notNull(command, WorkflowCode.DEFINITION_INVALID);
        Require.notNull(command.getDefinitionId(), WorkflowCode.DEFINITION_INVALID.getCode(), "流程定义ID不能为空");
        WorkflowDefinition definition = definitionMapper.selectById(command.getDefinitionId());
        Require.notNull(definition, WorkflowCode.DEFINITION_NOT_FOUND);
        Require.isTrue(WorkflowDefinitionStatus.PUBLISHED.name().equals(definition.getStatus()),
                WorkflowCode.DEFINITION_STATUS_INVALID.getCode(), "只有已发布流程可以发起");
        Require.notBlank(definition.getProcessDefinitionId(), WorkflowCode.DEFINITION_STATUS_INVALID.getCode(),
                "流程未部署到引擎，请先发布");

        Map<String, Object> variables = new HashMap<>();
        if (command.getVariables() != null) {
            variables.putAll(command.getVariables());
        }
        if (StringUtils.hasText(command.getBusinessType())) {
            variables.put(BUSINESS_TYPE_VAR, command.getBusinessType().trim());
        }
        if (command.getApplyId() != null) {
            variables.put(APPLY_ID_VAR, String.valueOf(command.getApplyId()));
        }
        if (command.getSelectedAssignees() != null && !command.getSelectedAssignees().isEmpty()) {
            variables.put("mangoSelectedAssignees", command.getSelectedAssignees());
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
        workflowBusinessApplyService.markProcessStarted(applyId, definition.getId(), definition.getDefinitionKey(),
                definition.getProcessDefinitionId(), definition.getDefinitionName(), instance.getProcessInstanceId());
        workflowTaskRuntimeService.advanceRuntimeTasks(instance.getProcessInstanceId());

        WorkflowProcessInstanceVO vo = new WorkflowProcessInstanceVO();
        vo.setProcessInstanceId(instance.getProcessInstanceId());
        vo.setBusinessKey(instance.getBusinessKey());
        vo.setDefinitionId(definition.getId());
        vo.setProcessName(definition.getDefinitionName());
        vo.setProcessKey(definition.getDefinitionKey());
        vo.setProcessDefinitionId(definition.getProcessDefinitionId());
        vo.setInitiatorName(initiator);
        fillCurrentTask(instance.getProcessInstanceId(), vo);
        vo.setStatus(WorkflowInstanceStatus.RUNNING.getLabel());
        vo.setStartTime(LocalDateTime.now());
        return R.ok(vo);
    }

    @Override
    public R<PageResult<WorkflowProcessInstanceVO>> initiated(WorkflowTaskPageQuery query) {
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
        return R.ok(PageResult.of(records, total, resolved.getPage(), resolved.getSize()));
    }

    @Override
    public R<WorkflowProcessDetailVO> detail(String processInstanceId) {
        return workflowTaskRuntimeService.processDetail(processInstanceId);
    }

    @Override
    public R<PageResult<WorkflowProcessInstanceVO>> historyByBusinessKey(String businessKey, WorkflowTaskPageQuery query) {
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
        return R.ok(PageResult.of(records, total, resolved.getPage(), resolved.getSize()));
    }

    @Override
    public List<WorkflowBusinessProcessVO> latestByBusinessKeys(Collection<String> businessKeys) {
        if (businessKeys == null || businessKeys.isEmpty()) {
            return List.of();
        }
        Map<String, io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO> applyProgress =
                workflowBusinessApplyService.latestProgress(null, businessKeys);
        if (!applyProgress.isEmpty()) {
            return applyProgress.values().stream()
                    .map(this::fromApplyProgress)
                    .toList();
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
        vo.setProcessInstanceId(progress.getProcessInstanceId());
        vo.setProcessName(progress.getProcessName());
        vo.setCurrentTaskName(progress.getCurrentTaskNames());
        vo.setCurrentTaskDefinitionKey(progress.getCurrentTaskDefinitionKeys());
        vo.setStatus(progress.getApplyStatusName());
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

    private Long resolveApplyId(StartWorkflowProcessCommand command, WorkflowDefinition definition,
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
                ? WorkflowApplyRenderMode.DYNAMIC_FORM
                : command.getRenderMode());
        applyCommand.setApplyPageKey(trim(command.getApplyPageKey()));
        applyCommand.setApprovePageKey(trim(command.getApprovePageKey()));
        applyCommand.setFormKey(definition.getFormCode());
        applyCommand.setFormVersion(definition.getPublishedVersionNo());
        applyCommand.setFormJsonSnapshot(definition.getFormJson());
        applyCommand.setSnapshotRef(trim(command.getSnapshotRef()));
        applyCommand.setVariables(variables);
        WorkflowBusinessApplyVO apply = workflowBusinessApplyService.create(applyCommand).getData();
        return apply == null ? null : apply.getId();
    }

    private WorkflowProcessInstanceVO fromHistoricInstance(HistoricProcessInstance instance) {
        WorkflowProcessInstanceVO vo = new WorkflowProcessInstanceVO();
        vo.setProcessInstanceId(instance.getId());
        vo.setBusinessKey(instance.getBusinessKey());
        vo.setProcessName(instance.getProcessDefinitionName());
        vo.setProcessKey(instance.getProcessDefinitionKey());
        vo.setProcessDefinitionId(instance.getProcessDefinitionId());
        WorkflowFormInstance formInstance = formInstanceMapper.selectOne(new LambdaQueryWrapper<WorkflowFormInstance>()
                .eq(WorkflowFormInstance::getProcessInstanceId, instance.getId())
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

    private void saveFormInstance(WorkflowDefinition definition, ProcessInstance instance, Map<String, Object> variables) {
        LocalDateTime now = LocalDateTime.now();
        WorkflowFormInstance formInstance = new WorkflowFormInstance();
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
        WorkflowTaskRecord record = new WorkflowTaskRecord();
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
