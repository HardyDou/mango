package io.mango.workflow.core.event;

import io.mango.common.result.Require;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.entity.WorkflowFormInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
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
            WorkflowDefinition definition,
            ProcessInstance instance,
            Map<String, Object> variables) {
        Require.notNull(definition, "流程定义不能为空");
        Require.notNull(instance, "流程实例不能为空");
        Map<String, Object> payload = basePayload(instance.getProcessInstanceId(), variables);
        payload.put("processDefinitionId", instance.getProcessDefinitionId());
        payload.put("definitionId", definition.getId());
        payload.put("definitionKey", definition.getDefinitionKey());
        payload.put("definitionName", definition.getDefinitionName());
        publish(WorkflowDomainEvents.PROCESS_STARTED, instance.getBusinessKey(), variables, payload);
    }

    public void publishTaskCompleted(
            Task task,
            WorkflowFormInstance formInstance,
            Map<String, Object> variables,
            String comment) {
        publishTaskEvent(WorkflowDomainEvents.TASK_COMPLETED, task, formInstance, variables, comment);
    }

    public void publishTaskRejected(
            Task task,
            WorkflowFormInstance formInstance,
            Map<String, Object> variables,
            String comment) {
        publishTaskEvent(WorkflowDomainEvents.TASK_REJECTED, task, formInstance, variables, comment);
    }

    public void publishProcessCompleted(
            String processInstanceId,
            WorkflowFormInstance formInstance,
            Map<String, Object> variables) {
        publishProcessEvent(WorkflowDomainEvents.PROCESS_COMPLETED, processInstanceId, formInstance, variables);
    }

    public void publishProcessRejected(
            String processInstanceId,
            WorkflowFormInstance formInstance,
            Map<String, Object> variables,
            String reason) {
        Map<String, Object> payload = basePayload(processInstanceId, variables);
        payload.put("reason", reason);
        publish(WorkflowDomainEvents.PROCESS_REJECTED, businessKey(formInstance, variables), variables, payload);
    }

    public void publishProcessEnded(
            String processInstanceId,
            WorkflowFormInstance formInstance,
            Map<String, Object> variables,
            String reason) {
        Map<String, Object> payload = basePayload(processInstanceId, variables);
        payload.put("reason", reason);
        publish(WorkflowDomainEvents.PROCESS_ENDED, businessKey(formInstance, variables), variables, payload);
    }

    private void publishTaskEvent(
            String eventType,
            Task task,
            WorkflowFormInstance formInstance,
            Map<String, Object> variables,
            String comment) {
        Require.notNull(task, "任务不能为空");
        Map<String, Object> payload = basePayload(task.getProcessInstanceId(), variables);
        payload.put("taskId", task.getId());
        payload.put("taskName", task.getName());
        payload.put("taskDefinitionKey", task.getTaskDefinitionKey());
        payload.put("assignee", task.getAssignee());
        payload.put("comment", comment);
        publish(eventType, businessKey(formInstance, variables), variables, payload);
    }

    private void publishProcessEvent(
            String eventType,
            String processInstanceId,
            WorkflowFormInstance formInstance,
            Map<String, Object> variables) {
        Map<String, Object> payload = basePayload(processInstanceId, variables);
        publish(eventType, businessKey(formInstance, variables), variables, payload);
    }

    private void publish(
            String eventType,
            String businessKey,
            Map<String, Object> variables,
            Map<String, Object> payload) {
        IDomainEventPublisher publisher = publisherProvider.getIfAvailable();
        if (publisher == null) {
            return;
        }
        publisher.publish(DomainEvent.builder()
                .eventType(eventType)
                .businessType(stringVar(variables, VAR_BUSINESS_TYPE))
                .businessKey(businessKey)
                .aggregateId(stringVar(variables, VAR_APPLY_ID))
                .payload(payload)
                .build());
    }

    private Map<String, Object> basePayload(String processInstanceId, Map<String, Object> variables) {
        Require.notBlank(processInstanceId, "流程实例ID不能为空");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("processInstanceId", processInstanceId);
        payload.put("businessType", stringVar(variables, VAR_BUSINESS_TYPE));
        payload.put("businessKey", stringVar(variables, VAR_BUSINESS_KEY));
        payload.put("applyId", stringVar(variables, VAR_APPLY_ID));
        payload.put("variables", variables == null ? Map.of() : new LinkedHashMap<>(variables));
        return payload;
    }

    private String businessKey(WorkflowFormInstance formInstance, Map<String, Object> variables) {
        if (formInstance != null && formInstance.getBusinessKey() != null && !formInstance.getBusinessKey().isBlank()) {
            return formInstance.getBusinessKey();
        }
        return stringVar(variables, VAR_BUSINESS_KEY);
    }

    private String stringVar(Map<String, Object> variables, String key) {
        Object value = variables == null ? null : variables.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
