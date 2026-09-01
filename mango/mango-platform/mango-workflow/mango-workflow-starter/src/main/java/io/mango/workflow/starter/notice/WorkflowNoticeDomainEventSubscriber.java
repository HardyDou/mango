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
    private static final int C0_CONTROL_LIMIT = 0x20;
    private static final int DELETE_CONTROL_CODE_POINT = 0x7f;
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            WorkflowEventTypes.TASK_ADVANCED,
            WorkflowEventTypes.PROCESS_COMPLETED,
            WorkflowEventTypes.PROCESS_REJECTED);

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
        if (WorkflowEventTypes.TASK_ADVANCED.equals(event.getEventType())) {
            publishTaskAdvancedNotices(event, bizType, params);
            return;
        }
        RecipientSelection recipients = processTerminalEvent(event.getEventType())
                ? applicantRecipient(params)
                : taskRecipients(params);
        publishNotice(event, bizType, params, recipients, null);
    }

    private void publishTaskAdvancedNotices(DomainEvent event, String bizType, Map<String, Object> params) {
        if (Boolean.TRUE.equals(params.get("ended"))) {
            log.debug("Skip task assigned notice for ended process. eventId={}, businessKey={}",
                    event.getEventId(), event.getBusinessKey());
            return;
        }
        Object currentTasks = params.get("currentTasks");
        if (currentTasks instanceof Collection<?> tasks) {
            for (Object currentTask : tasks) {
                if (currentTask instanceof Map<?, ?> task) {
                    Map<String, Object> taskParams = taskParams(params, task);
                    publishNotice(event, bizType, taskParams, taskRecipients(taskParams),
                            stringValue(taskParams.get("taskId")));
                }
            }
            return;
        }
        publishNotice(event, bizType, params, taskRecipients(params), stringValue(params.get("taskId")));
    }

    private void publishNotice(
            DomainEvent event,
            String bizType,
            Map<String, Object> params,
            RecipientSelection recipients,
            String taskId) {
        if (recipients.isEmpty()) {
            log.debug("Skip workflow notice without active recipients. eventId={}, eventType={}, taskId={}, businessKey={}",
                    event.getEventId(), event.getEventType(), taskId, event.getBusinessKey());
            return;
        }
        NoticeSiteMessageTargetCommand target = target(event, params);
        NoticeSendEventCommand notice = new NoticeSendEventCommand();
        notice.setTenantId(stringValue(payload(event).get("tenantId")));
        notice.setAppCode(stringValue(payload(event).get("appCode")));
        notice.setRealm(stringValue(payload(event).get("realm")));
        notice.setBizType(bizType);
        notice.setBizId(firstText(event.getBusinessKey(), stringValue(payload(event).get("processInstanceId"))));
        notice.setRecipientRuleCode(RECIPIENT_RULE_CODE);
        notice.setPriority(priority(event.getEventType()));
        notice.setIdempotentKey("workflow:" + event.getEventId()
                + (StringUtils.hasText(taskId) ? ":" + taskId.trim() : ""));
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

    private Map<String, Object> taskParams(Map<String, Object> params, Map<?, ?> task) {
        Map<String, Object> resolved = new LinkedHashMap<>(params);
        resolved.remove("taskId");
        resolved.remove("taskDefinitionKey");
        resolved.remove("taskName");
        resolved.remove("currentTaskId");
        resolved.remove("assignee");
        resolved.remove("assigneeId");
        resolved.remove("assigneeName");
        resolved.remove("claimStatus");
        resolved.remove("candidateUsers");
        resolved.remove("candidateGroups");
        task.forEach((key, value) -> {
            if (key != null) {
                resolved.put(String.valueOf(key), value);
            }
        });
        resolved.put("assignee", resolved.get("assigneeId"));
        resolved.put("currentTaskId", resolved.get("taskId"));
        resolved.put("currentTask", task);
        return resolved;
    }

    private String toNoticeBizType(String eventType) {
        return switch (eventType) {
            case WorkflowEventTypes.PROCESS_COMPLETED -> "workflow.process.completed";
            case WorkflowEventTypes.PROCESS_REJECTED -> "workflow.process.rejected";
            case WorkflowEventTypes.TASK_ADVANCED -> "workflow.task.assigned";
            default -> null;
        };
    }

    private NoticePriority priority(String eventType) {
        if (WorkflowEventTypes.PROCESS_REJECTED.equals(eventType)) {
            return NoticePriority.HIGH;
        }
        return NoticePriority.NORMAL;
    }

    private Map<String, Object> toParams(DomainEvent event) {
        Map<String, Object> params = new LinkedHashMap<>(payload(event));
        params.put("eventType", event.getEventType());
        params.put("businessType", event.getBusinessType());
        params.put("businessKey", event.getBusinessKey());
        params.put("processName", firstText(stringValue(params.get("definitionName")), "流程"));
        params.put("applyTitle", firstText(stringValue(params.get("applyTitle")), "业务申请"));
        if (WorkflowEventTypes.PROCESS_REJECTED.equals(event.getEventType())) {
            params.put("reason", firstText(stringValue(params.get("reason")), "未填写"));
        }
        return params;
    }

    private NoticeSiteMessageSubjectCommand subject(DomainEvent event, Map<String, Object> params) {
        NoticeSiteMessageSubjectCommand subject = new NoticeSiteMessageSubjectCommand();
        subject.setSubjectType("WORKFLOW_PROCESS");
        subject.setSubjectId(firstText(stringValue(params.get("processInstanceId")), event.getAggregateId(), event.getBusinessKey()));
        subject.setSubjectName(firstText(stringValue(params.get("processName")), "流程"));
        return subject;
    }

    private NoticeSiteMessageTargetCommand target(DomainEvent event, Map<String, Object> params) {
        NoticeSiteMessageTargetCommand target = new NoticeSiteMessageTargetCommand();
        target.setTargetType(NoticeSiteMessageTargetType.ROUTE);
        String fallbackTargetKey = defaultWorkflowTargetKey(event.getEventType(), params);
        String targetKey = workflowTargetKey(event.getEventType(), params);
        Map<String, Object> targetParams = new LinkedHashMap<>(params);
        if (!targetKey.equals(fallbackTargetKey)) {
            targetParams.put("fallbackTargetKey", fallbackTargetKey);
        }
        target.setTargetKey(targetKey);
        target.setParams(NoticeJsonRequest.of(targetParams));
        return target;
    }

    private String workflowTargetKey(String eventType, Map<String, Object> params) {
        if (processTerminalEvent(eventType)) {
            String viewPath = safeInternalPath(stringValue(params.get("viewPath")));
            if (StringUtils.hasText(viewPath)) {
                return viewPath;
            }
        }
        return defaultWorkflowTargetKey(eventType, params);
    }

    private String defaultWorkflowTargetKey(String eventType, Map<String, Object> params) {
        if (WorkflowEventTypes.TASK_ADVANCED.equals(eventType)
                && StringUtils.hasText(stringValue(params.get("taskId")))) {
            return "workflow:task:detail";
        }
        if (WorkflowEventTypes.PROCESS_COMPLETED.equals(eventType)) {
            return "workflow:task:done";
        }
        if (WorkflowEventTypes.PROCESS_REJECTED.equals(eventType)) {
            return "workflow:task:initiated";
        }
        return "workflow:task:todo";
    }

    private String safeInternalPath(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String path = value.trim();
        if (!path.startsWith("/") || path.startsWith("//") || path.contains("\\")
                || path.contains("?") || path.contains("#")) {
            return null;
        }
        return path.chars().anyMatch(character -> character < C0_CONTROL_LIMIT
                || character == DELETE_CONTROL_CODE_POINT) ? null : path;
    }

    private String actionLabel(String eventType) {
        if (processTerminalEvent(eventType)) {
            return "查看申请";
        }
        return "去审批";
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

    private RecipientSelection taskRecipients(Map<String, Object> payload) {
        Set<Long> userIds = new LinkedHashSet<>();
        Map<String, NoticeRecipientTargetCommand> targets = new LinkedHashMap<>();
        Long assigneeId = firstLong(payload.get("assigneeId"), payload.get("assignee"));
        if (assigneeId != null) {
            return new RecipientSelection(List.of(assigneeId), List.of());
        }
        addCandidateUsers(userIds, payload.get("candidateUsers"));
        addCandidateGroups(targets, payload.get("candidateGroups"));
        return new RecipientSelection(List.copyOf(userIds), List.copyOf(targets.values()));
    }

    private RecipientSelection applicantRecipient(Map<String, Object> payload) {
        Long applicantId = parseLong(stringValue(payload.get("applicantId")));
        return applicantId == null
                ? new RecipientSelection(List.of(), List.of())
                : new RecipientSelection(List.of(applicantId), List.of());
    }

    private boolean processTerminalEvent(String eventType) {
        return WorkflowEventTypes.PROCESS_COMPLETED.equals(eventType)
                || WorkflowEventTypes.PROCESS_REJECTED.equals(eventType);
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

    private Long firstLong(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            Long parsed = parseLong(stringValue(value));
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
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
