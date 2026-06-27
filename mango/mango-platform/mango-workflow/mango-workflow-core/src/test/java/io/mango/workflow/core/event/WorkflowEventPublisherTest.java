package io.mango.workflow.core.event;

import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.vo.WorkflowBusinessApplyCurrentTaskVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
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
    void publishTaskAdvanced_shouldIncludeAdvancedSnapshot() {
        TaskEntityImpl task = task();
        WorkflowFormInstance formInstance = new WorkflowFormInstance();
        formInstance.setBusinessKey("EXP-FORM-KEY");

        publisher.publishTaskAdvanced(task, formInstance, variables(), "同意", false, businessApply());

        DomainEvent event = singleEvent();
        assertThat(event.getEventType()).isEqualTo(WorkflowDomainEvents.TASK_ADVANCED);
        assertThat(event.getBusinessKey()).isEqualTo("EXP-FORM-KEY");
        assertThat(event.getPayload())
                .containsEntry("processInstanceId", "PROC-1")
                .containsEntry("completedTaskId", "TASK-1")
                .containsEntry("completedTaskDefinitionKey", "manager_approve")
                .containsEntry("completedTaskName", "部门经理审批")
                .containsEntry("comment", "同意")
                .containsEntry("ended", false)
                .containsEntry("applyId", 1001L)
                .containsEntry("businessType", "EXPENSE_REIMBURSEMENT")
                .containsEntry("businessKey", "EXP-20260516-001")
                .containsEntry("applyStatus", WorkflowApplyStatus.IN_APPROVAL.name())
                .containsEntry("currentTaskNames", "财务审批")
                .containsEntry("currentTaskDefinitionKeys", "finance_approve")
                .containsEntry("currentAssigneeNames", "lisi")
                .containsEntry("assignee", 1002L)
                .containsEntry("assigneeName", "lisi");
        assertThat((List<?>) event.getPayload().get("currentTasks")).singleElement()
                .satisfies(currentTask -> {
                    Map<?, ?> currentTaskPayload = (Map<?, ?>) currentTask;
                    assertThat(currentTaskPayload.get("taskId")).isEqualTo("TASK-2");
                    assertThat(currentTaskPayload.get("taskDefinitionKey")).isEqualTo("finance_approve");
                    assertThat(currentTaskPayload.get("taskName")).isEqualTo("财务审批");
                    assertThat(currentTaskPayload.get("assigneeId")).isEqualTo(1002L);
                    assertThat(currentTaskPayload.get("assigneeName")).isEqualTo("lisi");
                });
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

    private WorkflowBusinessApplyVO businessApply() {
        WorkflowBusinessApplyCurrentTaskVO currentTask = new WorkflowBusinessApplyCurrentTaskVO();
        currentTask.setTaskId("TASK-2");
        currentTask.setTaskDefinitionKey("finance_approve");
        currentTask.setTaskName("财务审批");
        currentTask.setAssigneeId(1002L);
        currentTask.setAssigneeName("lisi");

        WorkflowBusinessApplyVO businessApply = new WorkflowBusinessApplyVO();
        businessApply.setId(1001L);
        businessApply.setBusinessType("EXPENSE_REIMBURSEMENT");
        businessApply.setBusinessKey("EXP-20260516-001");
        businessApply.setApplyStatus(WorkflowApplyStatus.IN_APPROVAL);
        businessApply.setApplyStatusName("审批中");
        businessApply.setCurrentTaskNames("财务审批");
        businessApply.setCurrentTaskDefinitionKeys("finance_approve");
        businessApply.setCurrentAssigneeNames("lisi");
        businessApply.setCurrentTasks(List.of(currentTask));
        return businessApply;
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
