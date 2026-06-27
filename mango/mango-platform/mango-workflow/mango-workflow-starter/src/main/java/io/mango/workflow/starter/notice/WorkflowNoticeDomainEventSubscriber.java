package io.mango.workflow.starter.notice;

import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.DomainEventSubscriber;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.event.NoticeSendEvent;
import io.mango.workflow.api.WorkflowEventTypes;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Converts workflow domain events into decoupled notice send events.
 */
@Component
public class WorkflowNoticeDomainEventSubscriber implements DomainEventSubscriber {

    private static final String RECIPIENT_RULE_CODE = "workflow.operator";
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            WorkflowEventTypes.TASK_ADVANCED,
            WorkflowEventTypes.TASK_REJECTED,
            WorkflowEventTypes.PROCESS_COMPLETED,
            WorkflowEventTypes.PROCESS_REJECTED,
            WorkflowEventTypes.PROCESS_ENDED);

    private final ApplicationEventPublisher eventPublisher;

    public WorkflowNoticeDomainEventSubscriber(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String eventType() {
        return "*";
    }

    @Override
    public void onEvent(DomainEvent event) {
        if (event == null || !SUPPORTED_EVENTS.contains(event.getEventType())) {
            return;
        }
        String bizType = toNoticeBizType(event.getEventType());
        if (!StringUtils.hasText(bizType)) {
            return;
        }
        NoticeSendEvent.NoticeSendEventBuilder builder = NoticeSendEvent.builder()
                .bizType(bizType)
                .bizId(firstText(event.getBusinessKey(), stringValue(payload(event).get("processInstanceId"))))
                .recipientRuleCode(RECIPIENT_RULE_CODE)
                .priority(priority(event.getEventType()))
                .idempotentKey("workflow:" + event.getEventId())
                .params(toParams(event));
        Long assigneeId = parseLong(stringValue(payload(event).get("assignee")));
        if (assigneeId != null) {
            builder.userId(assigneeId);
        }
        eventPublisher.publishEvent(builder.build());
    }

    private String toNoticeBizType(String eventType) {
        return switch (eventType) {
            case WorkflowEventTypes.TASK_REJECTED -> "workflow.task.rejected";
            case WorkflowEventTypes.PROCESS_COMPLETED -> "workflow.process.completed";
            case WorkflowEventTypes.PROCESS_REJECTED -> "workflow.process.rejected";
            case WorkflowEventTypes.PROCESS_ENDED -> "workflow.process.ended";
            case WorkflowEventTypes.TASK_ADVANCED -> "workflow.task.assigned";
            default -> null;
        };
    }

    private NoticePriority priority(String eventType) {
        if (WorkflowEventTypes.TASK_REJECTED.equals(eventType)
                || WorkflowEventTypes.PROCESS_REJECTED.equals(eventType)) {
            return NoticePriority.HIGH;
        }
        return NoticePriority.NORMAL;
    }

    private Map<String, Object> toParams(DomainEvent event) {
        Map<String, Object> params = new LinkedHashMap<>(payload(event));
        params.put("eventType", event.getEventType());
        params.put("businessType", event.getBusinessType());
        params.put("businessKey", event.getBusinessKey());
        params.putIfAbsent("processName", firstText(stringValue(params.get("definitionName")),
                stringValue(params.get("businessType")), "流程"));
        return params;
    }

    private Map<String, Object> payload(DomainEvent event) {
        return event.getPayload() == null ? Map.of() : event.getPayload();
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
