package io.mango.workflow.core.event;

import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.entity.WorkflowFormInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowEventPublisherTest {

    private final List<DomainEvent> events = new ArrayList<>();
    private final WorkflowEventPublisher publisher = new WorkflowEventPublisher(
            new SingleObjectProvider(event -> events.add(event)));

    @Test
    void publishProcessStarted_shouldIncludeBusinessRoutingFields() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(1001L);
        definition.setDefinitionKey("expense_reimbursement");
        definition.setDefinitionName("费用报销");

        ExecutionEntityImpl instance = new ExecutionEntityImpl();
        instance.setProcessInstanceId("PROC-1");
        instance.setBusinessKey("EXP-20260516-001");
        instance.setProcessDefinitionId("flowable-def-1");

        publisher.publishProcessStarted(definition, instance, variables());

        DomainEvent event = singleEvent();
        assertThat(event.getEventType()).isEqualTo(WorkflowDomainEvents.PROCESS_STARTED);
        assertThat(event.getBusinessType()).isEqualTo("EXPENSE_REIMBURSEMENT");
        assertThat(event.getBusinessKey()).isEqualTo("EXP-20260516-001");
        assertThat(event.getAggregateId()).isEqualTo("APPLY-1");
        assertThat(event.getPayload())
                .containsEntry("processInstanceId", "PROC-1")
                .containsEntry("definitionId", 1001L)
                .containsEntry("definitionKey", "expense_reimbursement")
                .containsEntry("definitionName", "费用报销");
    }

    @Test
    void publishTaskCompleted_shouldPreferFormInstanceBusinessKey() {
        TaskEntityImpl task = task();
        WorkflowFormInstance formInstance = new WorkflowFormInstance();
        formInstance.setBusinessKey("EXP-FORM-KEY");

        publisher.publishTaskCompleted(task, formInstance, variables(), "同意");

        DomainEvent event = singleEvent();
        assertThat(event.getEventType()).isEqualTo(WorkflowDomainEvents.TASK_COMPLETED);
        assertThat(event.getBusinessKey()).isEqualTo("EXP-FORM-KEY");
        assertThat(event.getPayload())
                .containsEntry("processInstanceId", "PROC-1")
                .containsEntry("taskId", "TASK-1")
                .containsEntry("taskName", "部门经理审批")
                .containsEntry("taskDefinitionKey", "manager_approve")
                .containsEntry("assignee", "zhangsan")
                .containsEntry("comment", "同意");
    }

    @Test
    void publishProcessRejected_shouldPublishRejectedAndEndedEvents() {
        WorkflowFormInstance formInstance = new WorkflowFormInstance();
        formInstance.setBusinessKey("EXP-FORM-KEY");

        publisher.publishProcessRejected("PROC-1", formInstance, variables(), "票据不完整");
        publisher.publishProcessEnded("PROC-1", formInstance, variables(), "票据不完整");

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo(WorkflowDomainEvents.PROCESS_REJECTED);
        assertThat(events.get(1).getEventType()).isEqualTo(WorkflowDomainEvents.PROCESS_ENDED);
        assertThat(events)
                .allSatisfy(event -> {
                    assertThat(event.getBusinessType()).isEqualTo("EXPENSE_REIMBURSEMENT");
                    assertThat(event.getBusinessKey()).isEqualTo("EXP-FORM-KEY");
                    assertThat(event.getAggregateId()).isEqualTo("APPLY-1");
                    assertThat(event.getPayload()).containsEntry("reason", "票据不完整");
                });
    }

    private DomainEvent singleEvent() {
        assertThat(events).hasSize(1);
        return events.get(0);
    }

    private Map<String, Object> variables() {
        return Map.of(
                "businessType", "EXPENSE_REIMBURSEMENT",
                "businessKey", "EXP-20260516-001",
                "applyId", "APPLY-1",
                "amount", 1280);
    }

    private TaskEntityImpl task() {
        TaskEntityImpl task = new TaskEntityImpl();
        task.setId("TASK-1");
        task.setName("部门经理审批");
        task.setTaskDefinitionKey("manager_approve");
        task.setProcessInstanceId("PROC-1");
        task.setAssignee("zhangsan");
        return task;
    }

    private record SingleObjectProvider(IDomainEventPublisher publisher)
            implements ObjectProvider<IDomainEventPublisher> {

        @Override
        public IDomainEventPublisher getObject(Object... args) {
            return publisher;
        }

        @Override
        public IDomainEventPublisher getIfAvailable() {
            return publisher;
        }

        @Override
        public IDomainEventPublisher getIfUnique() {
            return publisher;
        }

        @Override
        public IDomainEventPublisher getObject() {
            return publisher;
        }
    }
}
