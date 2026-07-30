package io.mango.workflow.starter.notice;

import io.mango.infra.event.api.DomainEvent;
import io.mango.notice.api.command.NoticeRecipientTargetCommand;
import io.mango.notice.api.command.NoticeSendEventCommand;
import io.mango.notice.api.enums.NoticeRecipientTargetType;
import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import io.mango.workflow.api.WorkflowEventTypes;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowNoticeDomainEventSubscriberTest {

    private final List<Object> events = new ArrayList<>();
    private final WorkflowNoticeDomainEventSubscriber subscriber =
            new WorkflowNoticeDomainEventSubscriber(new RecordingPublisher(events));

    @Test
    void taskAdvancedShouldPublishStructuredWorkflowNotice() {
        DomainEvent event = DomainEvent.builder()
                .eventId("event-1001")
                .eventType(WorkflowEventTypes.TASK_ADVANCED)
                .businessType("expense")
                .businessKey("EXP-1001")
                .aggregateId("PI-1001")
                .payload("processInstanceId", "PI-1001")
                .payload("taskId", "TASK-1001")
                .payload("assignee", "1001")
                .payload("tenantId", "1")
                .payload("appCode", "internal-admin")
                .payload("realm", "INTERNAL")
                .payload("definitionName", "费用报销")
                .build();

        subscriber.onEvent(event);

        assertThat(events).singleElement().satisfies(published -> {
            NoticeSendEventCommand notice = (NoticeSendEventCommand) published;
            assertThat(notice.getTenantId()).isEqualTo("1");
            assertThat(notice.getAppCode()).isEqualTo("internal-admin");
            assertThat(notice.getRealm()).isEqualTo("INTERNAL");
            assertThat(notice.getBizType()).isEqualTo("workflow.task.assigned");
            assertThat(notice.getUserId()).isEqualTo(1001L);
            assertThat(notice.getIdempotentKey()).isEqualTo("workflow:event-1001:TASK-1001");
            assertThat(notice.getMessageScene()).isEqualTo("workflow.task.assigned");
            assertThat(notice.getMessageSubject().getSubjectType()).isEqualTo("WORKFLOW_PROCESS");
            assertThat(notice.getMessageSubject().getSubjectId()).isEqualTo("PI-1001");
            assertThat(notice.getMessageTarget().getTargetType()).isEqualTo(NoticeSiteMessageTargetType.ROUTE);
            assertThat(notice.getMessageTarget().getTargetKey()).isEqualTo("workflow:task:detail");
            assertThat(notice.getMessageData().toMap()).containsEntry("taskId", "TASK-1001");
            assertThat(notice.getMessageActions()).singleElement().satisfies(action -> {
                assertThat(action.getActionCode()).isEqualTo("OPEN_WORKFLOW");
                assertThat(action.getActionLabel()).isEqualTo("去审批");
                assertThat(action.getInteractionType()).isEqualTo(NoticeSiteMessageActionInteractionType.ROUTE);
            });
        });
    }

    @Test
    void taskAdvancedShouldKeepSharedCandidateTaskAsOneNotice() {
        DomainEvent event = DomainEvent.builder()
                .eventId("event-1002")
                .eventType(WorkflowEventTypes.TASK_ADVANCED)
                .businessType("guarantee")
                .businessKey("GUARANTEE-1002")
                .aggregateId("PI-1002")
                .payload("processInstanceId", "PI-1002")
                .payload("taskId", "TASK-1002")
                .payload("tenantId", "1")
                .payload("ended", false)
                .payload("currentTasks", List.of(
                        Map.of(
                                "taskId", "TASK-1002",
                                "taskName", "财务审批",
                                "candidateUsers", List.of("1002", "invalid-user"),
                                "candidateGroups", List.of(
                                        "ROLE:2001", "POST:3001", "ORG:4002", "ORG_LEADER:4003"))))
                .build();

        subscriber.onEvent(event);

        assertThat(events).singleElement().satisfies(published -> {
            NoticeSendEventCommand notice = (NoticeSendEventCommand) published;
            assertThat(notice.getUserId()).isEqualTo(1002L);
            assertThat(notice.getUserIds()).isNull();
            assertThat(notice.getIdempotentKey()).isEqualTo("workflow:event-1002:TASK-1002");
            assertThat(notice.getMessageData().toMap()).containsEntry("taskId", "TASK-1002");
            assertThat(notice.getRecipientTargets())
                    .extracting(NoticeRecipientTargetCommand::getTargetType,
                            NoticeRecipientTargetCommand::getTargetId)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(NoticeRecipientTargetType.ROLE, 2001L),
                            org.assertj.core.groups.Tuple.tuple(NoticeRecipientTargetType.POST, 3001L),
                            org.assertj.core.groups.Tuple.tuple(NoticeRecipientTargetType.ORG, 4002L));
        });
    }

    @Test
    void taskAdvancedShouldPublishOneNoticePerAssignedRuntimeTask() {
        DomainEvent event = DomainEvent.builder()
                .eventId("event-1003")
                .eventType(WorkflowEventTypes.TASK_ADVANCED)
                .businessType("expense")
                .businessKey("EXP-1003")
                .payload("processInstanceId", "PI-1003")
                .payload("tenantId", "1")
                .payload("ended", false)
                .payload("currentTasks", List.of(
                        Map.of(
                                "taskId", "TASK-1003-A",
                                "assigneeId", 1003L,
                                "candidateUsers", List.of("9001"),
                                "candidateGroups", List.of("ROLE:2001")),
                        Map.of(
                                "taskId", "TASK-1003-B",
                                "assigneeId", 1004L,
                                "candidateUsers", List.of("9002"))))
                .build();

        subscriber.onEvent(event);

        assertThat(events).hasSize(2);
        NoticeSendEventCommand first = (NoticeSendEventCommand) events.get(0);
        NoticeSendEventCommand second = (NoticeSendEventCommand) events.get(1);
        assertThat(first.getUserId()).isEqualTo(1003L);
        assertThat(first.getRecipientTargets()).isNull();
        assertThat(first.getMessageData().toMap())
                .containsEntry("taskId", "TASK-1003-A")
                .containsEntry("currentTaskId", "TASK-1003-A");
        assertThat(first.getIdempotentKey()).isEqualTo("workflow:event-1003:TASK-1003-A");
        assertThat(second.getUserId()).isEqualTo(1004L);
        assertThat(second.getMessageData().toMap())
                .containsEntry("taskId", "TASK-1003-B")
                .containsEntry("currentTaskId", "TASK-1003-B");
        assertThat(second.getIdempotentKey()).isEqualTo("workflow:event-1003:TASK-1003-B");
    }

    @Test
    void terminalProcessShouldNotifyApplicantOnly() {
        DomainEvent event = DomainEvent.builder()
                .eventId("event-1004")
                .eventType(WorkflowEventTypes.PROCESS_COMPLETED)
                .businessType("expense")
                .businessKey("EXP-1004")
                .payload("processInstanceId", "PI-1004")
                .payload("tenantId", "1")
                .payload("applicantId", 8001L)
                .payload("assigneeId", 9001L)
                .payload("candidateGroups", List.of("ROLE:2001"))
                .build();

        subscriber.onEvent(event);

        assertThat(events).singleElement().satisfies(published -> {
            NoticeSendEventCommand notice = (NoticeSendEventCommand) published;
            assertThat(notice.getUserId()).isEqualTo(8001L);
            assertThat(notice.getUserIds()).isNull();
            assertThat(notice.getRecipientTargets()).isNull();
            assertThat(notice.getBizType()).isEqualTo("workflow.process.completed");
        });
    }

    @Test
    void terminalProcessShouldPreferSafeCustomViewPathAndKeepWorkflowFallback() {
        DomainEvent event = DomainEvent.builder()
                .eventId("event-view-path")
                .eventType(WorkflowEventTypes.PROCESS_COMPLETED)
                .businessType("expense")
                .businessKey("EXP-VIEW-1")
                .payload("processInstanceId", "PI-VIEW-1")
                .payload("applicantId", 8001L)
                .payload("viewPath", "/expense/apply/detail")
                .build();

        subscriber.onEvent(event);

        assertThat(events).singleElement().satisfies(published -> {
            NoticeSendEventCommand notice = (NoticeSendEventCommand) published;
            assertThat(notice.getMessageTarget().getTargetKey()).isEqualTo("/expense/apply/detail");
            assertThat(notice.getMessageTarget().getParams().toMap())
                    .containsEntry("fallbackTargetKey", "workflow:task:done")
                    .containsEntry("processInstanceId", "PI-VIEW-1");
            assertThat(notice.getMessageActions()).singleElement()
                    .satisfies(action -> assertThat(action.getActionLabel()).isEqualTo("查看申请"));
        });
    }

    @Test
    void terminalProcessShouldRejectExternalViewPathAndUseWorkflowFallback() {
        DomainEvent event = DomainEvent.builder()
                .eventId("event-unsafe-view-path")
                .eventType(WorkflowEventTypes.PROCESS_REJECTED)
                .businessType("expense")
                .businessKey("EXP-VIEW-2")
                .payload("processInstanceId", "PI-VIEW-2")
                .payload("applicantId", 8001L)
                .payload("viewPath", "https://example.com/detail")
                .build();

        subscriber.onEvent(event);

        assertThat(events).singleElement().satisfies(published -> {
            NoticeSendEventCommand notice = (NoticeSendEventCommand) published;
            assertThat(notice.getMessageTarget().getTargetKey()).isEqualTo("workflow:task:initiated");
            assertThat(notice.getMessageTarget().getParams().toMap()).doesNotContainKey("fallbackTargetKey");
        });
    }

    @Test
    void taskAdvancedWithoutNextTaskShouldNotPublishAssignedNotice() {
        DomainEvent event = DomainEvent.builder()
                .eventId("event-1005")
                .eventType(WorkflowEventTypes.TASK_ADVANCED)
                .businessType("guarantee")
                .businessKey("GUARANTEE-1005")
                .aggregateId("PI-1005")
                .payload("processInstanceId", "PI-1005")
                .payload("tenantId", "1")
                .payload("ended", true)
                .payload("currentTasks", List.of())
                .build();

        subscriber.onEvent(event);

        assertThat(events).isEmpty();
    }

    @Test
    void workflowNoticeWithoutResolvableRecipientShouldNotPublish() {
        DomainEvent event = DomainEvent.builder()
                .eventId("event-1006")
                .eventType(WorkflowEventTypes.PROCESS_ENDED)
                .businessType("expense")
                .businessKey("EXP-1006")
                .payload("processInstanceId", "PI-1006")
                .payload("tenantId", "1")
                .build();

        subscriber.onEvent(event);

        assertThat(events).isEmpty();
    }

    private record RecordingPublisher(List<Object> events) implements ApplicationEventPublisher {

        @Override
        public void publishEvent(ApplicationEvent event) {
            events.add(event);
        }

        @Override
        public void publishEvent(Object event) {
            events.add(event);
        }
    }
}
