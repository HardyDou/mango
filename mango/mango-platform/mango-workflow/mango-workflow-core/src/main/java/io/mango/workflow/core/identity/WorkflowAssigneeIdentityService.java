package io.mango.workflow.core.identity;

import io.mango.workflow.api.vo.WorkflowBusinessApplyCurrentTaskVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Workflow 办理人身份批量增强服务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowAssigneeIdentityService {

    private final ObjectProvider<IWorkflowAssigneeIdentityProvider> provider;

    public void enrichTasks(List<WorkflowTaskVO> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        Map<String, WorkflowAssigneeIdentity> identities = resolve(tasks.stream()
                .filter(Objects::nonNull)
                .map(WorkflowTaskVO::getAssigneeName)
                .toList());
        tasks.stream()
                .filter(Objects::nonNull)
                .forEach(task -> apply(task, identityFor(task.getAssigneeName(), identities)));
    }

    public void enrichCurrentTasks(List<WorkflowBusinessApplyCurrentTaskVO> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        Map<String, WorkflowAssigneeIdentity> identities = resolve(tasks.stream()
                .filter(Objects::nonNull)
                .map(WorkflowBusinessApplyCurrentTaskVO::getAssigneeName)
                .toList());
        enrichCurrentTasks(tasks, identities);
    }

    public void enrichBusinessApplies(Collection<WorkflowBusinessApplyVO> applies) {
        if (applies == null || applies.isEmpty()) {
            return;
        }
        List<WorkflowBusinessApplyCurrentTaskVO> tasks = applies.stream()
                .filter(apply -> apply != null && apply.getCurrentTasks() != null)
                .flatMap(apply -> apply.getCurrentTasks().stream())
                .toList();
        enrichCurrentTasks(tasks);
    }

    public void enrichProgresses(Collection<WorkflowBusinessApplyProgressVO> progresses) {
        if (progresses == null || progresses.isEmpty()) {
            return;
        }
        List<WorkflowBusinessApplyCurrentTaskVO> tasks = progresses.stream()
                .filter(progress -> progress != null && progress.getCurrentTasks() != null)
                .flatMap(progress -> progress.getCurrentTasks().stream())
                .toList();
        enrichCurrentTasks(tasks);
        progresses.forEach(this::copyFirstTaskIdentity);
    }

    public void enrichCurrentTasks(List<WorkflowBusinessApplyCurrentTaskVO> tasks,
                                   Map<String, WorkflowAssigneeIdentity> identities) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        Map<String, WorkflowAssigneeIdentity> resolved = identities == null ? Map.of() : identities;
        tasks.stream()
                .filter(Objects::nonNull)
                .forEach(task -> apply(task, identityFor(task.getAssigneeName(), resolved)));
    }

    public Map<String, WorkflowAssigneeIdentity> resolve(Collection<String> assigneeKeys) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (assigneeKeys != null) {
            assigneeKeys.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(keys::add);
        }
        if (keys.isEmpty()) {
            return Map.of();
        }
        try {
            IWorkflowAssigneeIdentityProvider identityProvider = provider.getIfAvailable();
            if (identityProvider == null) {
                return Map.of();
            }
            Map<String, WorkflowAssigneeIdentity> result = identityProvider.resolveAll(List.copyOf(keys));
            return result == null ? Map.of() : result;
        } catch (RuntimeException exception) {
            log.warn("Unable to batch resolve workflow assignee identities: assigneeCount={}", keys.size(), exception);
            return Map.of();
        }
    }

    private WorkflowAssigneeIdentity identityFor(String assigneeName,
                                                  Map<String, WorkflowAssigneeIdentity> identities) {
        return StringUtils.hasText(assigneeName) ? identities.get(assigneeName.trim()) : null;
    }

    private void apply(WorkflowTaskVO task, WorkflowAssigneeIdentity identity) {
        task.setAssigneeId(identity == null ? null : identity.userId());
        task.setAssigneeDisplayName(identity == null ? null : identity.displayName());
    }

    private void apply(WorkflowBusinessApplyCurrentTaskVO task, WorkflowAssigneeIdentity identity) {
        task.setAssigneeId(identity == null ? null : identity.userId());
        task.setAssigneeDisplayName(identity == null ? null : identity.displayName());
    }

    private void copyFirstTaskIdentity(WorkflowBusinessApplyProgressVO progress) {
        if (progress == null || progress.getCurrentTasks() == null || progress.getCurrentTasks().isEmpty()) {
            if (progress != null) {
                progress.setAssigneeId(null);
                progress.setAssigneeDisplayName(null);
            }
            return;
        }
        WorkflowBusinessApplyCurrentTaskVO first = progress.getCurrentTasks().getFirst();
        progress.setAssigneeId(first.getAssigneeId());
        progress.setAssigneeDisplayName(first.getAssigneeDisplayName());
    }
}
