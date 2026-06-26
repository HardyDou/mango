package io.mango.org.starter.resource;

import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import org.springframework.util.StringUtils;

final class ResourceFieldReader {

    private final String resourceType;

    ResourceFieldReader(String resourceType) {
        this.resourceType = resourceType;
    }

    String requiredString(ResourceDeclaration resource, String fieldName) {
        String value = stringField(resource, fieldName);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(resourceType + " field is required: " + fieldName);
        }
        return value.trim();
    }

    String stringField(ResourceDeclaration resource, String fieldName) {
        Object value = fieldValue(resource, fieldName);
        return value == null ? null : String.valueOf(value);
    }

    String stringField(ResourceDeclaration resource, String fieldName, String defaultValue) {
        String value = stringField(resource, fieldName);
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    Long requiredLong(ResourceDeclaration resource, String fieldName) {
        Long value = longField(resource, fieldName);
        if (value == null) {
            throw new IllegalStateException(resourceType + " field is required: " + fieldName);
        }
        return value;
    }

    Long longField(ResourceDeclaration resource, String fieldName) {
        Object value = fieldValue(resource, fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? Long.valueOf(text.trim()) : null;
    }

    Integer intField(ResourceDeclaration resource, String fieldName, Integer defaultValue) {
        Object value = fieldValue(resource, fieldName);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? Integer.valueOf(text.trim()) : defaultValue;
    }

    Object fieldValue(ResourceDeclaration resource, String fieldName) {
        if (resource.getFields() == null) {
            return null;
        }
        ResourceField field = resource.getFields().get(fieldName);
        return field == null ? null : field.getValue();
    }
}
