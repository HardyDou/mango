package io.mango.workflow.starter.notice;

import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.DomainEventSubscriber;
import io.mango.notice.api.command.NoticeSiteMessageActionCommand;
import io.mango.notice.api.command.NoticeSiteMessageSubjectCommand;
import io.mango.notice.api.command.NoticeSiteMessageTargetCommand;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import io.mango.notice.api.command.NoticeJsonRequest;
import io.mango.notice.api.command.NoticeSendEventCommand;
import io.mango.workflow.api.WorkflowEventTypes;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
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
        Map<String, Object> params = toParams(event);
        NoticeSiteMessageTargetCommand target = target(event, params);
        NoticeSendEventCommand notice = new NoticeSendEventCommand();
        notice.setTenantId(stringValue(payload(event).get("tenantId")));
        notice.setBizType(bizType);
        notice.setBizId(firstText(event.getBusinessKey(), stringValue(payload(event).get("processInstanceId"))));
        notice.setRecipientRuleCode(RECIPIENT_RULE_CODE);
        notice.setPriority(priority(event.getEventType()));
        notice.setIdempotentKey("workflow:" + event.getEventId());
        notice.setParams(NoticeJsonRequest.of(params));
        notice.setMessageScene(bizType);
        notice.setMessageSubject(subject(event, params));
        notice.setMessageTarget(target);
        notice.setMessageData(NoticeJsonRequest.of(params));
        notice.setMessageActions(List.of(routeAction(actionLabel(event.getEventType()), target)));
        Long assigneeId = parseLong(stringValue(payload(event).get("assignee")));
        if (assigneeId != null) {
            notice.setUserId(assigneeId);
        }
        eventPublisher.publishEvent(notice);
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

    private NoticeSiteMessageSubjectCommand subject(DomainEvent event, Map<String, Object> params) {
        NoticeSiteMessageSubjectCommand subject = new NoticeSiteMessageSubjectCommand();
        subject.setSubjectType("WORKFLOW_PROCESS");
        subject.setSubjectId(firstText(stringValue(params.get("processInstanceId")), event.getAggregateId(), event.getBusinessKey()));
        subject.setSubjectName(firstText(stringValue(params.get("processName")), event.getBusinessType(), "流程"));
        return subject;
    }

    private NoticeSiteMessageTargetCommand target(DomainEvent event, Map<String, Object> params) {
        NoticeSiteMessageTargetCommand target = new NoticeSiteMessageTargetCommand();
        target.setTargetType(NoticeSiteMessageTargetType.ROUTE);
        target.setTargetKey(workflowTargetKey(event.getEventType(), params));
        target.setParams(NoticeJsonRequest.of(params));
        return target;
    }

    private String workflowTargetKey(String eventType, Map<String, Object> params) {
        if ((WorkflowEventTypes.TASK_ADVANCED.equals(eventType) || WorkflowEventTypes.TASK_REJECTED.equals(eventType))
                && StringUtils.hasText(stringValue(params.get("taskId")))) {
            return "workflow:task:detail";
        }
        if (WorkflowEventTypes.PROCESS_COMPLETED.equals(eventType)
                || WorkflowEventTypes.PROCESS_ENDED.equals(eventType)) {
            return "workflow:task:done";
        }
        if (WorkflowEventTypes.PROCESS_REJECTED.equals(eventType)) {
            return "workflow:task:initiated";
        }
        return "workflow:task:todo";
    }

    private String actionLabel(String eventType) {
        if (WorkflowEventTypes.TASK_REJECTED.equals(eventType)
                || WorkflowEventTypes.PROCESS_REJECTED.equals(eventType)) {
            return "查看驳回";
        }
        if (WorkflowEventTypes.PROCESS_COMPLETED.equals(eventType)
                || WorkflowEventTypes.PROCESS_ENDED.equals(eventType)) {
            return "查看已办";
        }
        return "处理任务";
    }

    private NoticeSiteMessageActionCommand routeAction(String actionLabel, NoticeSiteMessageTargetCommand target) {
        NoticeSiteMessageActionCommand action = new NoticeSiteMessageActionCommand();
        action.setActionCode("OPEN_WORKFLOW");
        action.setActionLabel(actionLabel);
        action.setInteractionType(NoticeSiteMessageActionInteractionType.ROUTE);
        action.setTarget(target);
        return action;
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
