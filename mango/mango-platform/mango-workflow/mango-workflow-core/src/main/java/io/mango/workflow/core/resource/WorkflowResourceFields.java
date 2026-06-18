package io.mango.workflow.core.resource;

import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import org.springframework.util.StringUtils;

/**
 * Workflow 资源字段读取工具。
 */
final class WorkflowResourceFields {

    private WorkflowResourceFields() {
    }

    static String text(ResourceDeclaration resource, String name, boolean required) {
        Object value = value(resource, name, required);
        return value == null ? null : String.valueOf(value);
    }

    static String requiredText(ResourceDeclaration resource, String name, String resourceType) {
        String text = text(resource, name, true);
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException(resourceType + " field is required: " + name);
        }
        return text.trim();
    }

    static String defaultText(ResourceDeclaration resource, String name, String defaultValue) {
        String text = text(resource, name, false);
        return StringUtils.hasText(text) ? text.trim() : defaultValue;
    }

    static Long longValue(ResourceDeclaration resource, String name, boolean required, Long defaultValue) {
        Object value = value(resource, name, required);
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    static Integer intValue(ResourceDeclaration resource, String name, boolean required, Integer defaultValue) {
        Object value = value(resource, name, required);
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    static Object value(ResourceDeclaration resource, String name, boolean required) {
        ResourceField field = resource.getFields().get(name);
        Object value = field == null ? null : field.getValue();
        if (required && value == null) {
            throw new IllegalStateException(resource.getResourceType() + " field is required: " + name);
        }
        return value;
    }
}
