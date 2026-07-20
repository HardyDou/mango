package io.mango.workflow.starter.notice;

import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.DomainEventSubscriber;
import io.mango.notice.api.command.NoticeJsonRequest;
import io.mango.notice.api.command.NoticeRecipientTargetCommand;
import io.mango.notice.api.command.NoticeSendEventCommand;
import io.mango.notice.api.command.NoticeSiteMessageActionCommand;
import io.mango.notice.api.command.NoticeSiteMessageSubjectCommand;
import io.mango.notice.api.command.NoticeSiteMessageTargetCommand;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeRecipientTargetType;
import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import io.mango.workflow.api.WorkflowEventTypes;
import io.mango.workflow.core.engine.WorkflowAssigneeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts workflow domain events into decoupled notice send events.
 */
@Slf4j
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
        RecipientSelection recipients = recipients(params);
        if (WorkflowEventTypes.TASK_ADVANCED.equals(event.getEventType())
                && (Boolean.TRUE.equals(params.get("ended")) || recipients.isEmpty())) {
            log.debug("Skip task assigned notice without active recipients. eventId={}, businessKey={}",
                    event.getEventId(), event.getBusinessKey());
            return;
        }
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
        if (recipients.userIds().size() == 1) {
            notice.setUserId(recipients.userIds().getFirst());
        } else if (!recipients.userIds().isEmpty()) {
            notice.setUserIds(recipients.userIds());
        }
        if (!recipients.targets().isEmpty()) {
            notice.setRecipientTargets(recipients.targets());
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

    private RecipientSelection recipients(Map<String, Object> payload) {
        Set<Long> userIds = new LinkedHashSet<>();
        Map<String, NoticeRecipientTargetCommand> targets = new LinkedHashMap<>();
        addUserId(userIds, payload.get("assigneeId"));
        addUserId(userIds, payload.get("assignee"));
        addCandidateUsers(userIds, payload.get("candidateUsers"));
        addCandidateGroups(targets, payload.get("candidateGroups"));
        Object currentTasks = payload.get("currentTasks");
        if (currentTasks instanceof Collection<?> tasks) {
            for (Object currentTask : tasks) {
                if (currentTask instanceof Map<?, ?> task) {
                    addUserId(userIds, task.get("assigneeId"));
                    addCandidateUsers(userIds, task.get("candidateUsers"));
                    addCandidateGroups(targets, task.get("candidateGroups"));
                }
            }
        }
        return new RecipientSelection(List.copyOf(userIds), List.copyOf(targets.values()));
    }

    private void addCandidateUsers(Set<Long> userIds, Object value) {
        if (value instanceof Collection<?> candidates) {
            candidates.forEach(candidate -> addUserId(userIds, candidate));
        }
    }

    private void addUserId(Set<Long> userIds, Object value) {
        Long userId = parseLong(stringValue(value));
        if (userId != null) {
            userIds.add(userId);
        }
    }

    private void addCandidateGroups(Map<String, NoticeRecipientTargetCommand> targets, Object value) {
        if (!(value instanceof Collection<?> groups)) {
            return;
        }
        groups.stream()
                .map(this::recipientTarget)
                .filter(java.util.Objects::nonNull)
                .forEach(target -> targets.putIfAbsent(
                        target.getTargetType() + ":" + target.getTargetId(), target));
    }

    private NoticeRecipientTargetCommand recipientTarget(Object value) {
        String group = stringValue(value);
        if (!StringUtils.hasText(group)) {
            return null;
        }
        String normalized = group.trim();
        NoticeRecipientTargetType targetType;
        String targetId;
        if (normalized.startsWith(WorkflowAssigneeResolver.GROUP_ROLE_PREFIX)) {
            targetType = NoticeRecipientTargetType.ROLE;
            targetId = normalized.substring(WorkflowAssigneeResolver.GROUP_ROLE_PREFIX.length());
        } else if (normalized.startsWith(WorkflowAssigneeResolver.GROUP_POST_PREFIX)) {
            targetType = NoticeRecipientTargetType.POST;
            targetId = normalized.substring(WorkflowAssigneeResolver.GROUP_POST_PREFIX.length());
        } else if (normalized.startsWith(WorkflowAssigneeResolver.GROUP_ORG_PREFIX)) {
            targetType = NoticeRecipientTargetType.ORG;
            targetId = normalized.substring(WorkflowAssigneeResolver.GROUP_ORG_PREFIX.length());
        } else {
            return null;
        }
        Long parsedTargetId = parseLong(targetId);
        if (parsedTargetId == null) {
            return null;
        }
        NoticeRecipientTargetCommand target = new NoticeRecipientTargetCommand();
        target.setTargetType(targetType);
        target.setTargetId(parsedTargetId);
        return target;
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

    private record RecipientSelection(List<Long> userIds, List<NoticeRecipientTargetCommand> targets) {

        private boolean isEmpty() {
            return userIds.isEmpty() && targets.isEmpty();
        }
    }
}
