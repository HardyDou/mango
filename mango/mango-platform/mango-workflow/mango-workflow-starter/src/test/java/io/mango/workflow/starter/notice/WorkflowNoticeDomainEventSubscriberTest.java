package io.mango.workflow.starter.notice;

import io.mango.infra.event.api.DomainEvent;
import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import io.mango.notice.api.event.NoticeSendEvent;
import io.mango.workflow.api.WorkflowEventTypes;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

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
                .payload("definitionName", "费用报销")
                .build();

        subscriber.onEvent(event);

        assertThat(events).singleElement().satisfies(published -> {
            NoticeSendEvent notice = (NoticeSendEvent) published;
            assertThat(notice.getBizType()).isEqualTo("workflow.task.assigned");
            assertThat(notice.getUserId()).isEqualTo(1001L);
            assertThat(notice.getMessageScene()).isEqualTo("workflow.task.assigned");
            assertThat(notice.getMessageSubject().getSubjectType()).isEqualTo("WORKFLOW_PROCESS");
            assertThat(notice.getMessageSubject().getSubjectId()).isEqualTo("PI-1001");
            assertThat(notice.getMessageTarget().getTargetType()).isEqualTo(NoticeSiteMessageTargetType.ROUTE);
            assertThat(notice.getMessageTarget().getTargetKey()).isEqualTo("workflow:task:detail");
            assertThat(notice.getMessageData()).containsEntry("taskId", "TASK-1001");
            assertThat(notice.getMessageActions()).singleElement().satisfies(action -> {
                assertThat(action.getActionCode()).isEqualTo("OPEN_WORKFLOW");
                assertThat(action.getActionLabel()).isEqualTo("处理任务");
                assertThat(action.getInteractionType()).isEqualTo(NoticeSiteMessageActionInteractionType.ROUTE);
            });
        });
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
