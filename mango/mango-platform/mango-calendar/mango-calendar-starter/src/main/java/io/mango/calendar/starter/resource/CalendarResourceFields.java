package io.mango.calendar.starter.resource;

import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.springframework.util.StringUtils;

final class CalendarResourceFields {

    private CalendarResourceFields() {
    }

    static String requiredText(ResourceDeclaration resource, String name) {
        Object value = requiredValue(resource, name);
        if (!(value instanceof String text) || !StringUtils.hasText(text)) {
            throw new IllegalArgumentException("Calendar resource field must be non-blank: " + name);
        }
        return text.trim();
    }

    static int requiredInt(ResourceDeclaration resource, String name) {
        Object value = requiredValue(resource, name);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return Integer.parseInt(text.trim());
        }
        throw new IllegalArgumentException("Calendar resource field must be an integer: " + name);
    }

    static Object optionalValue(ResourceDeclaration resource, String name) {
        if (resource == null || resource.getFields() == null) {
            return null;
        }
        ResourceField field = resource.getFields().get(name);
        return field == null ? null : field.getValue();
    }

    private static Object requiredValue(ResourceDeclaration resource, String name) {
        Object value = optionalValue(resource, name);
        if (value == null) {
            throw new IllegalArgumentException("Missing calendar resource field: " + name);
        }
        return value;
    }
}
