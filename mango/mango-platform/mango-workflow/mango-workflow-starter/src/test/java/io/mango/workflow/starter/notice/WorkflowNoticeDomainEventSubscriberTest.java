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
                .payload("definitionName", "费用报销")
                .build();

        subscriber.onEvent(event);

        assertThat(events).singleElement().satisfies(published -> {
            NoticeSendEventCommand notice = (NoticeSendEventCommand) published;
            assertThat(notice.getTenantId()).isEqualTo("1");
            assertThat(notice.getBizType()).isEqualTo("workflow.task.assigned");
            assertThat(notice.getUserId()).isEqualTo(1001L);
            assertThat(notice.getMessageScene()).isEqualTo("workflow.task.assigned");
            assertThat(notice.getMessageSubject().getSubjectType()).isEqualTo("WORKFLOW_PROCESS");
            assertThat(notice.getMessageSubject().getSubjectId()).isEqualTo("PI-1001");
            assertThat(notice.getMessageTarget().getTargetType()).isEqualTo(NoticeSiteMessageTargetType.ROUTE);
            assertThat(notice.getMessageTarget().getTargetKey()).isEqualTo("workflow:task:detail");
            assertThat(notice.getMessageData().toMap()).containsEntry("taskId", "TASK-1001");
            assertThat(notice.getMessageActions()).singleElement().satisfies(action -> {
                assertThat(action.getActionCode()).isEqualTo("OPEN_WORKFLOW");
                assertThat(action.getActionLabel()).isEqualTo("处理任务");
                assertThat(action.getInteractionType()).isEqualTo(NoticeSiteMessageActionInteractionType.ROUTE);
            });
        });
    }

    @Test
    void taskAdvancedShouldResolveCandidateUsersGroupsAndParallelTasks() {
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
                .payload("candidateUsers", List.of("1002", "1003", "invalid-user"))
                .payload("candidateGroups", List.of("ROLE:2001", "POST:3001", "ORG_LEADER:4001"))
                .payload("currentTasks", List.of(
                        Map.of(
                                "taskId", "TASK-1002",
                                "assigneeId", 1004L,
                                "candidateUsers", List.of("1002"),
                                "candidateGroups", List.of("ROLE:2001")),
                        Map.of(
                                "taskId", "TASK-1003",
                                "candidateUsers", List.of("1005"),
                                "candidateGroups", List.of("ORG:4002"))))
                .build();

        subscriber.onEvent(event);

        assertThat(events).singleElement().satisfies(published -> {
            NoticeSendEventCommand notice = (NoticeSendEventCommand) published;
            assertThat(notice.getUserId()).isNull();
            assertThat(notice.getUserIds()).containsExactly(1002L, 1003L, 1004L, 1005L);
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
    void taskAdvancedWithoutNextTaskShouldNotPublishAssignedNotice() {
        DomainEvent event = DomainEvent.builder()
                .eventId("event-1003")
                .eventType(WorkflowEventTypes.TASK_ADVANCED)
                .businessType("guarantee")
                .businessKey("GUARANTEE-1003")
                .aggregateId("PI-1003")
                .payload("processInstanceId", "PI-1003")
                .payload("tenantId", "1")
                .payload("ended", true)
                .payload("currentTasks", List.of())
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
