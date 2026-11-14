package io.mango.workflow.core.engine;

import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.vo.AuthUserVO;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Flowable 表达式中使用的审批人集合工具。
 */
@Component("mangoWorkflowAssigneeCollection")
@RequiredArgsConstructor
public class WorkflowAssigneeCollection {

    public static final String EMPTY_ASSIGNEE = "__MANGO_EMPTY_ASSIGNEE__";

    private final WorkflowCandidateGroupProvider candidateGroupProvider;
    private final AuthUserProvider authUserProvider;

    public List<String> list(String assignees) {
        if (!StringUtils.hasText(assignees)) {
            return List.of();
        }
        return Arrays.stream(assignees.split("\\s*,\\s*"))
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    public List<String> selected(DelegateExecution execution, String nodeId) {
        Object selected = execution == null ? null : execution.getVariable("mangoSelectedAssignees");
        List<String> assignees = List.of();
        if (selected instanceof Map<?, ?> map) {
            Object value = map.get(nodeId);
            if (value == null) {
                value = map.get("default");
            }
            assignees = valueList(value);
        }
        return assignees.isEmpty() ? List.of(EMPTY_ASSIGNEE) : assignees;
    }

    public List<String> formUsers(DelegateExecution execution, String formUserField) {
        Object value = execution == null || !StringUtils.hasText(formUserField) ? null : execution.getVariable(formUserField);
        List<String> assignees = valueList(value);
        return assignees.isEmpty() ? List.of(EMPTY_ASSIGNEE) : assignees;
    }

    /**
     * 把逗号分隔的候选组展开为当前租户启用成员的登录名。
     *
     * <p>流程进入节点时求值，角色、岗位、组织成员变化对下一次提交立即生效。
     * 展开结果为空时返回 {@link #EMPTY_ASSIGNEE}，交由节点“审批人为空”策略处理，
     * 禁止返回空集合导致多实例节点静默通过。
     */
    public List<String> groupMembers(String groups) {
        if (!StringUtils.hasText(groups)) {
            return List.of(EMPTY_ASSIGNEE);
        }
        Set<String> members = new LinkedHashSet<>();
        for (String group : groups.split("\\s*,\\s*")) {
            if (!StringUtils.hasText(group)) {
                continue;
            }
            for (Long userId : candidateGroupProvider.memberUserIds(group.trim())) {
                if (userId != null) {
                    members.add(username(userId));
                }
            }
        }
        return members.isEmpty() ? List.of(EMPTY_ASSIGNEE) : List.copyOf(members);
    }

    private String username(Long userId) {
        AuthUserVO user = authUserProvider.getByIdForAuth(userId);
        return user != null && StringUtils.hasText(user.getUsername())
                ? user.getUsername()
                : String.valueOf(userId);
    }

    private List<String> valueList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).filter(StringUtils::hasText).distinct().toList();
        }
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            List<String> values = new ArrayList<>();
            for (Object item : array) {
                if (item != null && StringUtils.hasText(String.valueOf(item))) {
                    values.add(String.valueOf(item));
                }
            }
            return values.stream().distinct().toList();
        }
        String text = String.valueOf(value);
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        return Arrays.stream(text.split("\\s*,\\s*")).filter(StringUtils::hasText).distinct().toList();
    }
}
