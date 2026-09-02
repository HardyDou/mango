package io.mango.workflow.core.event;

import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowTaskClaimStatus;
import io.mango.workflow.api.vo.WorkflowBusinessApplyCurrentTaskVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.core.entity.WorkflowDefinitionEntity;
import io.mango.workflow.core.entity.WorkflowFormInstanceEntity;
import io.mango.workflow.core.identity.IWorkflowAssigneeIdentityProvider;
import io.mango.workflow.core.identity.WorkflowAssigneeIdentityService;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowEventPublisherTest {

    private final List<DomainEvent> events = new ArrayList<>();
    private final WorkflowEventPublisher publisher = new WorkflowEventPublisher(
            new SingleObjectProvider(event -> events.add(event)), identityService());

    private static WorkflowAssigneeIdentityService identityService() {
        @SuppressWarnings("unchecked")
        ObjectProvider<IWorkflowAssigneeIdentityProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return new WorkflowAssigneeIdentityService(provider);
    }

    @BeforeEach
    void setUpContext() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                99L, "1", "operator", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 10L,
                "internal-admin"));
    }

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void publishProcessStarted_shouldIncludeBusinessRoutingFields() {
        WorkflowDefinitionEntity definition = new WorkflowDefinitionEntity();
        definition.setId(1001L);
        definition.setDefinitionKey("expense_reimbursement");
        definition.setDefinitionName("费用报销");

        ExecutionEntityImpl instance = new ExecutionEntityImpl();
        instance.setProcessInstanceId("PROC-1");
        instance.setBusinessKey("EXP-20260516-001");
        instance.setProcessDefinitionId("flowable-def-1");

        publisher.publishProcessStarted(definition, instance, variables(), businessApply());

        DomainEvent event = singleEvent();
        assertThat(event.getEventType()).isEqualTo(WorkflowDomainEvents.PROCESS_STARTED);
        assertThat(event.getBusinessType()).isEqualTo("EXPENSE_REIMBURSEMENT");
        assertThat(event.getBusinessKey()).isEqualTo("EXP-20260516-001");
        assertThat(event.getAggregateId()).isEqualTo("APPLY-1");
        assertThat(event.getPayload())
                .containsEntry("eventType", WorkflowDomainEvents.PROCESS_STARTED)
                .containsEntry("processInstanceId", "PROC-1")
                .containsEntry("tenantId", "1")
                .containsEntry("appCode", "internal-admin")
                .containsEntry("realm", "INTERNAL")
                .containsEntry("definitionId", 1001L)
                .containsEntry("definitionKey", "expense_reimbursement")
                .containsEntry("definitionName", "费用报销")
                .containsEntry("applyTitle", "5月费用报销")
                .containsEntry("applySummary", "差旅及办公费用");
    }

    @Test
    void publishTaskCompleted_shouldPreferFormInstanceBusinessKey() {
        TaskEntityImpl task = task();
        WorkflowFormInstanceEntity formInstance = new WorkflowFormInstanceEntity();
        formInstance.setBusinessKey("EXP-FORM-KEY");

        publisher.publishTaskCompleted(task, formInstance, variables(), "同意");

        DomainEvent event = singleEvent();
        assertThat(event.getEventType()).isEqualTo(WorkflowDomainEvents.TASK_COMPLETED);
        assertThat(event.getBusinessKey()).isEqualTo("EXP-FORM-KEY");
        assertThat(event.getPayload())
                .containsEntry("eventType", WorkflowDomainEvents.TASK_COMPLETED)
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
        WorkflowFormInstanceEntity formInstance = new WorkflowFormInstanceEntity();
        formInstance.setBusinessKey("EXP-FORM-KEY");

        publisher.publishTaskAdvanced(task, formInstance, variables(), "同意", false, businessApply());

        DomainEvent event = singleEvent();
        assertThat(event.getEventType()).isEqualTo(WorkflowDomainEvents.TASK_ADVANCED);
        assertThat(event.getBusinessKey()).isEqualTo("EXP-FORM-KEY");
        assertThat(event.getPayload())
                .containsEntry("eventType", WorkflowDomainEvents.TASK_ADVANCED)
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
                .containsEntry("applicantId", 1000L)
                .containsEntry("applicantName", "申请人")
                .containsEntry("applyTitle", "5月费用报销")
                .containsEntry("applySummary", "差旅及办公费用")
                .containsEntry("definitionName", "费用报销")
                .containsEntry("viewPath", "/expense/apply/detail")
                .containsEntry("currentTaskNames", "财务审批")
                .containsEntry("currentTaskDefinitionKeys", "finance_approve")
                .containsEntry("currentAssigneeNames", "lisi")
                .containsEntry("assignee", "lisi")
                .containsEntry("assigneeName", "lisi")
                .containsEntry("assigneeDisplayName", "李四")
                .containsEntry("claimStatus", WorkflowTaskClaimStatus.ASSIGNED.name())
                .containsEntry("candidateUsers", List.of("1002"))
                .containsEntry("candidateGroups", List.of("finance"));
        assertThat((List<?>) event.getPayload().get("currentTasks")).singleElement()
                .satisfies(currentTask -> {
                    Map<?, ?> currentTaskPayload = (Map<?, ?>) currentTask;
                    assertThat(currentTaskPayload.get("taskId")).isEqualTo("TASK-2");
                    assertThat(currentTaskPayload.get("taskDefinitionKey")).isEqualTo("finance_approve");
                    assertThat(currentTaskPayload.get("taskName")).isEqualTo("财务审批");
                    assertThat(currentTaskPayload.get("assigneeId")).isEqualTo(1002L);
                    assertThat(currentTaskPayload.get("assigneeName")).isEqualTo("lisi");
                    assertThat(currentTaskPayload.get("assigneeDisplayName")).isEqualTo("李四");
                    assertThat(currentTaskPayload.get("claimStatus")).isEqualTo(WorkflowTaskClaimStatus.ASSIGNED.name());
                    assertThat(currentTaskPayload.get("candidateUsers")).isEqualTo(List.of("1002"));
                    assertThat(currentTaskPayload.get("candidateGroups")).isEqualTo(List.of("finance"));
                });
    }

    @Test
    void publishTaskArrived_shouldIncludeInitialTaskAndReadableApplicationSnapshot() {
        WorkflowFormInstanceEntity formInstance = new WorkflowFormInstanceEntity();
        formInstance.setBusinessKey("EXP-FORM-KEY");

        publisher.publishTaskArrived("PROC-1", formInstance, variables(), businessApply());

        DomainEvent event = singleEvent();
        assertThat(event.getEventType()).isEqualTo(WorkflowDomainEvents.TASK_ADVANCED);
        assertThat(event.getPayload())
                .containsEntry("ended", false)
                .containsEntry("definitionName", "费用报销")
                .containsEntry("applyTitle", "5月费用报销")
                .containsEntry("taskId", "TASK-2")
                .containsEntry("assigneeId", 1002L);
    }

    @Test
    void publishProcessRejected_shouldPublishRejectedAndEndedEvents() {
        WorkflowFormInstanceEntity formInstance = new WorkflowFormInstanceEntity();
        formInstance.setBusinessKey("EXP-FORM-KEY");

        WorkflowBusinessApplyVO businessApply = businessApply();
        publisher.publishProcessRejected("PROC-1", formInstance, variables(), "票据不完整", businessApply);
        publisher.publishProcessEnded("PROC-1", formInstance, variables(), "票据不完整", businessApply);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo(WorkflowDomainEvents.PROCESS_REJECTED);
        assertThat(events.get(1).getEventType()).isEqualTo(WorkflowDomainEvents.PROCESS_ENDED);
        assertThat(events)
                .allSatisfy(event -> {
                    assertThat(event.getBusinessType()).isEqualTo("EXPENSE_REIMBURSEMENT");
                    assertThat(event.getBusinessKey()).isEqualTo("EXP-FORM-KEY");
                    assertThat(event.getAggregateId()).isEqualTo("APPLY-1");
                    assertThat(event.getPayload())
                            .containsEntry("reason", "票据不完整")
                            .containsEntry("applicantId", 1000L)
                            .containsEntry("applicantName", "申请人")
                            .containsEntry("appCode", "internal-admin")
                            .containsEntry("realm", "INTERNAL");
                });
    }

    @Test
    void publishProcessWithdrawn_shouldIncludeTerminalBusinessAndOperatorSnapshot() {
        WorkflowFormInstanceEntity formInstance = new WorkflowFormInstanceEntity();
        formInstance.setBusinessKey("EXP-FORM-KEY");
        WorkflowBusinessApplyVO businessApply = businessApply();
        businessApply.setApplyStatus(WorkflowApplyStatus.WITHDRAWN);
        businessApply.setApplyStatusName("已撤回");
        businessApply.setCurrentTaskNames(null);
        businessApply.setCurrentTaskDefinitionKeys(null);
        businessApply.setCurrentAssigneeNames(null);
        businessApply.setCurrentTasks(List.of());

        publisher.publishProcessWithdrawn(
                "PROC-1", formInstance, variables(), "申请资料有误", businessApply);

        DomainEvent event = singleEvent();
        assertThat(event.getEventType()).isEqualTo(WorkflowDomainEvents.PROCESS_WITHDRAWN);
        assertThat(event.getBusinessType()).isEqualTo("EXPENSE_REIMBURSEMENT");
        assertThat(event.getBusinessKey()).isEqualTo("EXP-FORM-KEY");
        assertThat(event.getAggregateId()).isEqualTo("APPLY-1");
        assertThat(event.getPayload())
                .containsEntry("eventType", WorkflowDomainEvents.PROCESS_WITHDRAWN)
                .containsEntry("processInstanceId", "PROC-1")
                .containsEntry("tenantId", "1")
                .containsEntry("appCode", "internal-admin")
                .containsEntry("realm", "INTERNAL")
                .containsEntry("applyId", 1001L)
                .containsEntry("businessType", "EXPENSE_REIMBURSEMENT")
                .containsEntry("businessKey", "EXP-20260516-001")
                .containsEntry("applyStatus", WorkflowApplyStatus.WITHDRAWN.name())
                .containsEntry("applyStatusName", "已撤回")
                .containsEntry("applicantId", 1000L)
                .containsEntry("applicantName", "申请人")
                .containsEntry("reason", "申请资料有误")
                .containsEntry("ended", true)
                .containsEntry("claimStatus", "NONE")
                .containsEntry("currentTasks", List.of());
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
        currentTask.setAssigneeDisplayName("李四");
        currentTask.setClaimStatus(WorkflowTaskClaimStatus.ASSIGNED);
        currentTask.setCandidateUsers(List.of("1002"));
        currentTask.setCandidateGroups(List.of("finance"));

        WorkflowBusinessApplyVO businessApply = new WorkflowBusinessApplyVO();
        businessApply.setId(1001L);
        businessApply.setBusinessType("EXPENSE_REIMBURSEMENT");
        businessApply.setBusinessKey("EXP-20260516-001");
        businessApply.setApplyTitle("5月费用报销");
        businessApply.setApplySummary("差旅及办公费用");
        businessApply.setApplicantId(1000L);
        businessApply.setApplicantName("申请人");
        businessApply.setProcessDefinitionId(1001L);
        businessApply.setProcessDefinitionKey("expense_reimbursement");
        businessApply.setEngineProcessDefinitionId("flowable-def-1");
        businessApply.setProcessName("费用报销");
        businessApply.setViewPath("/expense/apply/detail");
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
