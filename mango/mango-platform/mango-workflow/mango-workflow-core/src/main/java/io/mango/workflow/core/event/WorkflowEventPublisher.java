package io.mango.workflow.core.event;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.workflow.api.vo.WorkflowEventPayloadVO;
import io.mango.workflow.api.vo.WorkflowJsonVO;
import io.mango.workflow.core.entity.WorkflowDefinitionEntity;
import io.mango.workflow.core.entity.WorkflowFormInstanceEntity;
import io.mango.workflow.api.vo.WorkflowBusinessApplyCurrentTaskVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Publishes standard workflow domain events.
 */
@Component
public class WorkflowEventPublisher {

    private static final String VAR_BUSINESS_TYPE = "businessType";
    private static final String VAR_BUSINESS_KEY = "businessKey";
    private static final String VAR_APPLY_ID = "applyId";

    private final ObjectProvider<IDomainEventPublisher> publisherProvider;

    public WorkflowEventPublisher(ObjectProvider<IDomainEventPublisher> publisherProvider) {
        Require.notNull(publisherProvider, "领域事件发布器提供者不能为空");
        this.publisherProvider = publisherProvider;
    }

    public void publishProcessStarted(
            WorkflowDefinitionEntity definition,
            ProcessInstance instance,
            Map<String, Object> variables) {
        Require.notNull(definition, "流程定义不能为空");
        Require.notNull(instance, "流程实例不能为空");
        WorkflowEventPayloadVO payload = basePayload(WorkflowDomainEvents.PROCESS_STARTED,
                instance.getProcessInstanceId(), variables);
        payload.setProcessDefinitionId(instance.getProcessDefinitionId());
        payload.setDefinitionId(definition.getId());
        payload.setDefinitionKey(definition.getDefinitionKey());
        payload.setDefinitionName(definition.getDefinitionName());
        publish(WorkflowDomainEvents.PROCESS_STARTED, instance.getBusinessKey(), variables, payload);
    }

    public void publishTaskCompleted(
            Task task,
            WorkflowFormInstanceEntity formInstance,
            Map<String, Object> variables,
            String comment) {
        publishTaskEvent(WorkflowDomainEvents.TASK_COMPLETED, task, formInstance, variables, comment);
    }

    public void publishTaskAdvanced(
            Task completedTask,
            WorkflowFormInstanceEntity formInstance,
            Map<String, Object> variables,
            String comment,
            boolean ended,
            WorkflowBusinessApplyVO businessApply) {
        Require.notNull(completedTask, "已完成任务不能为空");
        WorkflowEventPayloadVO payload = basePayload(WorkflowDomainEvents.TASK_ADVANCED,
                completedTask.getProcessInstanceId(), variables);
        payload.setCompletedTaskId(completedTask.getId());
        payload.setCompletedTaskDefinitionKey(completedTask.getTaskDefinitionKey());
        payload.setCompletedTaskName(completedTask.getName());
        payload.setComment(comment);
        payload.setEnded(ended);
        putBusinessApply(payload, businessApply);
        publish(WorkflowDomainEvents.TASK_ADVANCED, businessKey(formInstance, variables), variables, payload);
    }

    public void publishTaskRejected(
            Task task,
            WorkflowFormInstanceEntity formInstance,
            Map<String, Object> variables,
            String comment) {
        publishTaskEvent(WorkflowDomainEvents.TASK_REJECTED, task, formInstance, variables, comment);
    }

    public void publishTaskSaved(
            Task task,
            WorkflowFormInstanceEntity formInstance,
            Map<String, Object> variables,
            String comment,
            WorkflowBusinessApplyVO businessApply) {
        publishTaskEvent(WorkflowDomainEvents.TASK_SAVED, task, formInstance, variables, comment, businessApply);
    }

    public void publishTaskClaimed(
            Task task,
            WorkflowFormInstanceEntity formInstance,
            Map<String, Object> variables,
            WorkflowBusinessApplyVO businessApply) {
        publishTaskEvent(WorkflowDomainEvents.TASK_CLAIMED, task, formInstance, variables, null, businessApply);
    }

    public void publishTaskUnclaimed(
            Task task,
            WorkflowFormInstanceEntity formInstance,
            Map<String, Object> variables,
            WorkflowBusinessApplyVO businessApply) {
        publishTaskEvent(WorkflowDomainEvents.TASK_UNCLAIMED, task, formInstance, variables, null, businessApply);
    }

    public void publishProcessCompleted(
            String processInstanceId,
            WorkflowFormInstanceEntity formInstance,
            Map<String, Object> variables) {
        publishProcessEvent(WorkflowDomainEvents.PROCESS_COMPLETED, processInstanceId, formInstance, variables);
    }

    public void publishProcessRejected(
            String processInstanceId,
            WorkflowFormInstanceEntity formInstance,
            Map<String, Object> variables,
            String reason) {
        WorkflowEventPayloadVO payload = basePayload(WorkflowDomainEvents.PROCESS_REJECTED, processInstanceId, variables);
        payload.setReason(reason);
        publish(WorkflowDomainEvents.PROCESS_REJECTED, businessKey(formInstance, variables), variables, payload);
    }

    public void publishProcessEnded(
            String processInstanceId,
            WorkflowFormInstanceEntity formInstance,
            Map<String, Object> variables,
            String reason) {
        WorkflowEventPayloadVO payload = basePayload(WorkflowDomainEvents.PROCESS_ENDED, processInstanceId, variables);
        payload.setReason(reason);
        publish(WorkflowDomainEvents.PROCESS_ENDED, businessKey(formInstance, variables), variables, payload);
    }

    private void publishTaskEvent(
            String eventType,
            Task task,
            WorkflowFormInstanceEntity formInstance,
            Map<String, Object> variables,
            String comment) {
        publishTaskEvent(eventType, task, formInstance, variables, comment, null);
    }

    private void publishTaskEvent(
            String eventType,
            Task task,
            WorkflowFormInstanceEntity formInstance,
            Map<String, Object> variables,
            String comment,
            WorkflowBusinessApplyVO businessApply) {
        Require.notNull(task, "任务不能为空");
        WorkflowEventPayloadVO payload = basePayload(eventType, task.getProcessInstanceId(), variables);
        payload.setTaskId(task.getId());
        payload.setTaskName(task.getName());
        payload.setTaskDefinitionKey(task.getTaskDefinitionKey());
        payload.setAssignee(task.getAssignee());
        payload.setAssigneeId(task.getAssignee());
        payload.setAssigneeName(task.getAssignee());
        payload.setComment(comment);
        putBusinessApply(payload, businessApply);
        publish(eventType, businessKey(formInstance, variables), variables, payload);
    }

    private void putBusinessApply(WorkflowEventPayloadVO payload, WorkflowBusinessApplyVO businessApply) {
        if (businessApply == null) {
            payload.setCurrentTasks(List.of());
            return;
        }
        List<WorkflowBusinessApplyCurrentTaskVO> currentTasks = businessApply.getCurrentTasks() == null
                ? List.of()
                : businessApply.getCurrentTasks();
        payload.setApplyId(businessApply.getId() == null ? null : String.valueOf(businessApply.getId()));
        payload.setBusinessType(businessApply.getBusinessType());
        payload.setBusinessKey(businessApply.getBusinessKey());
        payload.setApplyStatus(businessApply.getApplyStatus() == null ? null : businessApply.getApplyStatus().name());
        payload.setApplyStatusName(businessApply.getApplyStatusName());
        payload.setCurrentTaskNames(businessApply.getCurrentTaskNames());
        payload.setCurrentTaskDefinitionKeys(businessApply.getCurrentTaskDefinitionKeys());
        payload.setCurrentAssigneeNames(businessApply.getCurrentAssigneeNames());
        if (!currentTasks.isEmpty()) {
            WorkflowBusinessApplyCurrentTaskVO firstTask = currentTasks.getFirst();
            payload.setCurrentTask(firstTask);
            payload.setTaskId(firstTask.getTaskId());
            payload.setTaskDefinitionKey(firstTask.getTaskDefinitionKey());
            payload.setTaskName(firstTask.getTaskName());
            payload.setAssignee(firstTask.getAssigneeId() == null ? null : String.valueOf(firstTask.getAssigneeId()));
            payload.setAssigneeId(firstTask.getAssigneeId() == null ? null : String.valueOf(firstTask.getAssigneeId()));
            payload.setAssigneeName(firstTask.getAssigneeName());
            payload.setClaimStatus(firstTask.getClaimStatus() == null ? null : firstTask.getClaimStatus().name());
            payload.setCandidateUsers(firstTask.getCandidateUsers() == null ? List.of() : firstTask.getCandidateUsers());
            payload.setCandidateGroups(firstTask.getCandidateGroups() == null ? List.of() : firstTask.getCandidateGroups());
        } else {
            payload.setCurrentTask(null);
            payload.setClaimStatus("NONE");
            payload.setCandidateUsers(List.of());
            payload.setCandidateGroups(List.of());
        }
        payload.setCurrentTasks(currentTasks);
    }

    private Map<String, Object> currentTaskPayload(WorkflowBusinessApplyCurrentTaskVO task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getTaskId());
        payload.put("taskDefinitionKey", task.getTaskDefinitionKey());
        payload.put("taskName", task.getTaskName());
        payload.put("assigneeId", task.getAssigneeId());
        payload.put("assigneeName", task.getAssigneeName());
        payload.put("claimStatus", task.getClaimStatus() == null ? null : task.getClaimStatus().name());
        payload.put("candidateUsers", task.getCandidateUsers() == null ? List.of() : task.getCandidateUsers());
        payload.put("candidateGroups", task.getCandidateGroups() == null ? List.of() : task.getCandidateGroups());
        payload.put("arrivedAt", task.getArrivedAt());
        return payload;
    }

    private void publishProcessEvent(
            String eventType,
            String processInstanceId,
            WorkflowFormInstanceEntity formInstance,
            Map<String, Object> variables) {
        WorkflowEventPayloadVO payload = basePayload(eventType, processInstanceId, variables);
        publish(eventType, businessKey(formInstance, variables), variables, payload);
    }

    private void publish(
            String eventType,
            String businessKey,
            Map<String, Object> variables,
            WorkflowEventPayloadVO payload) {
        IDomainEventPublisher publisher = publisherProvider.getIfAvailable();
        if (publisher == null) {
            return;
        }
        publisher.publish(DomainEvent.builder()
                .eventType(eventType)
                .businessType(stringVar(variables, VAR_BUSINESS_TYPE))
                .businessKey(businessKey)
                .aggregateId(stringVar(variables, VAR_APPLY_ID))
                .payload(payloadMap(payload))
                .build());
    }

    private WorkflowEventPayloadVO basePayload(String eventType, String processInstanceId, Map<String, Object> variables) {
        Require.notBlank(processInstanceId, "流程实例ID不能为空");
        WorkflowEventPayloadVO payload = new WorkflowEventPayloadVO();
        payload.setEventType(eventType);
        payload.setProcessInstanceId(processInstanceId);
        payload.setTenantId(MangoContextHolder.tenantId());
        payload.setOperatorId(MangoContextHolder.userId());
        payload.setOperatorName(operatorName());
        payload.setBusinessType(stringVar(variables, VAR_BUSINESS_TYPE));
        payload.setBusinessKey(stringVar(variables, VAR_BUSINESS_KEY));
        payload.setApplyId(stringVar(variables, VAR_APPLY_ID));
        payload.setVariables(WorkflowJsonVO.of(variables));
        return payload;
    }

    private Map<String, Object> payloadMap(WorkflowEventPayloadVO payload) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventType", payload.getEventType());
        map.put("processInstanceId", payload.getProcessInstanceId());
        map.put("tenantId", payload.getTenantId());
        map.put("operatorId", payload.getOperatorId());
        map.put("operatorName", payload.getOperatorName());
        map.put("businessType", payload.getBusinessType());
        map.put("businessKey", payload.getBusinessKey());
        map.put("applyId", numericStringOrValue(payload.getApplyId()));
        map.put("variables", payload.getVariables() == null ? Map.of() : payload.getVariables().toMap());
        map.put("processDefinitionId", payload.getProcessDefinitionId());
        map.put("definitionId", payload.getDefinitionId());
        map.put("definitionKey", payload.getDefinitionKey());
        map.put("definitionName", payload.getDefinitionName());
        map.put("taskId", payload.getTaskId());
        map.put("taskName", payload.getTaskName());
        map.put("taskDefinitionKey", payload.getTaskDefinitionKey());
        map.put("assignee", numericStringOrValue(payload.getAssignee()));
        map.put("assigneeId", numericStringOrValue(payload.getAssigneeId()));
        map.put("assigneeName", payload.getAssigneeName());
        map.put("completedTaskId", payload.getCompletedTaskId());
        map.put("completedTaskDefinitionKey", payload.getCompletedTaskDefinitionKey());
        map.put("completedTaskName", payload.getCompletedTaskName());
        map.put("comment", payload.getComment());
        map.put("ended", payload.getEnded());
        map.put("reason", payload.getReason());
        map.put("applyStatus", payload.getApplyStatus());
        map.put("applyStatusName", payload.getApplyStatusName());
        map.put("currentTaskNames", payload.getCurrentTaskNames());
        map.put("currentTaskDefinitionKeys", payload.getCurrentTaskDefinitionKeys());
        map.put("currentAssigneeNames", payload.getCurrentAssigneeNames());
        map.put("currentTask", payload.getCurrentTask() == null ? null : currentTaskPayload(payload.getCurrentTask()));
        map.put("currentTaskId", payload.getCurrentTask() == null ? null : payload.getCurrentTask().getTaskId());
        map.put("claimStatus", payload.getClaimStatus());
        map.put("candidateUsers", payload.getCandidateUsers() == null ? List.of() : payload.getCandidateUsers());
        map.put("candidateGroups", payload.getCandidateGroups() == null ? List.of() : payload.getCandidateGroups());
        List<WorkflowBusinessApplyCurrentTaskVO> currentTasks = payload.getCurrentTasks() == null
                ? List.of()
                : payload.getCurrentTasks();
        map.put("currentTasks", currentTasks.stream()
                .map(this::currentTaskPayload)
                .toList());
        return map;
    }

    private String businessKey(WorkflowFormInstanceEntity formInstance, Map<String, Object> variables) {
        if (formInstance != null && formInstance.getBusinessKey() != null && !formInstance.getBusinessKey().isBlank()) {
            return formInstance.getBusinessKey();
        }
        return stringVar(variables, VAR_BUSINESS_KEY);
    }

    private String stringVar(Map<String, Object> variables, String key) {
        Object value = variables == null ? null : variables.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Object numericStringOrValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private String operatorName() {
        if (MangoContextHolder.principalName() != null && !MangoContextHolder.principalName().isBlank()) {
            return MangoContextHolder.principalName();
        }
        Long userId = MangoContextHolder.userId();
        return userId == null ? null : String.valueOf(userId);
    }
}
