package io.mango.workflow.core.engine;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Flowable 表达式中使用的审批人集合工具。
 */
@Component("mangoWorkflowAssigneeCollection")
public class WorkflowAssigneeCollection {

    public static final String EMPTY_ASSIGNEE = "__MANGO_EMPTY_ASSIGNEE__";

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
