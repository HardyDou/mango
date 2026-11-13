package io.mango.workflow.core.service.impl;

import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.vo.AuthUserVO;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 为待领取任务补齐运行时候选用户和候选组。 */
@Service
@RequiredArgsConstructor
public class WorkflowRuntimeCandidateLinker {

    private final TaskService taskService;
    private final AuthUserProvider authUserProvider;

    public boolean applyGroups(Task task, List<String> groups) {
        if (groups == null || groups.isEmpty() || !taskIdentityGroups(task.getId()).isEmpty()) {
            return false;
        }
        for (String group : groups) {
            taskService.addCandidateGroup(task.getId(), group);
        }
        return true;
    }

    public boolean applyUsers(Task task, List<String> users) {
        if (users == null || users.isEmpty()) {
            return false;
        }
        Set<String> existing = taskIdentityUsers(task.getId());
        boolean changed = false;
        for (String identifier : users) {
            String username = candidateUsername(identifier);
            if (StringUtils.hasText(username) && existing.add(username)) {
                taskService.addCandidateUser(task.getId(), username);
                changed = true;
            }
        }
        return changed;
    }

    private String candidateUsername(String identifier) {
        Long userId = resolveUserId(identifier);
        if (userId == null) {
            return StringUtils.hasText(identifier) ? identifier.trim() : null;
        }
        AuthUserVO user = authUserProvider.getByIdForAuth(userId);
        if (user != null && StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return StringUtils.hasText(identifier) ? identifier.trim() : null;
    }

    private Long resolveUserId(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return null;
        }
        try {
            return Long.valueOf(identifier.trim());
        } catch (NumberFormatException ignored) {
            AuthUserVO user = authUserProvider.getByUsernameForAuth(identifier.trim());
            return user == null ? null : user.getUserId();
        }
    }

    private Set<String> taskIdentityUsers(String taskId) {
        Set<String> users = new LinkedHashSet<>();
        List<IdentityLink> links = taskService.getIdentityLinksForTask(taskId);
        if (links == null) {
            return users;
        }
        for (IdentityLink link : links) {
            if (StringUtils.hasText(link.getUserId())) {
                users.add(link.getUserId());
            }
        }
        return users;
    }

    private Set<String> taskIdentityGroups(String taskId) {
        Set<String> groups = new LinkedHashSet<>();
        List<IdentityLink> links = taskService.getIdentityLinksForTask(taskId);
        if (links == null) {
            return groups;
        }
        for (IdentityLink link : links) {
            if (StringUtils.hasText(link.getGroupId())) {
                groups.add(link.getGroupId());
            }
        }
        return groups;
    }
}
