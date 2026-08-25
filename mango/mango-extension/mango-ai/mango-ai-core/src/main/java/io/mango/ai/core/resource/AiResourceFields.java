package io.mango.ai.core.resource;

import io.mango.common.result.Require;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;

import java.util.Arrays;

final class AiResourceFields {

    private AiResourceFields() {
    }

    static Long targetId(ResourceDeclaration resource) {
        String value = optionalText(resource, "targetId");
        if (value == null) {
            value = resource.getId();
        }
        Require.notBlank(value, resource.getResourceType() + " field is required: targetId");
        return Long.valueOf(value);
    }

    static String requiredText(ResourceDeclaration resource, String fieldName) {
        String value = optionalText(resource, fieldName);
        Require.notBlank(value, resource.getResourceType() + " field is required: " + fieldName);
        return value;
    }

    static String optionalText(ResourceDeclaration resource, String fieldName) {
        Object value = fieldValue(resource, fieldName);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    static int requiredInt(ResourceDeclaration resource, String fieldName) {
        return Integer.parseInt(requiredText(resource, fieldName));
    }

    static boolean requiredBoolean(ResourceDeclaration resource, String fieldName) {
        String value = requiredText(resource, fieldName);
        Require.isTrue("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value),
                resource.getResourceType() + " field must be boolean: " + fieldName);
        return Boolean.parseBoolean(value);
    }

    static <E extends Enum<E>> E requiredEnum(
            ResourceDeclaration resource,
            String fieldName,
            Class<E> enumType) {
        String value = requiredText(resource, fieldName);
        boolean supported = Arrays.stream(enumType.getEnumConstants())
                .anyMatch(item -> item.name().equals(value));
        Require.isTrue(supported,
                resource.getResourceType() + " field is invalid: " + fieldName + "=" + value);
        return Enum.valueOf(enumType, value);
    }

    static <E extends Enum<E>> E optionalEnum(
            ResourceDeclaration resource,
            String fieldName,
            Class<E> enumType) {
        String value = optionalText(resource, fieldName);
        if (value == null) {
            return null;
        }
        boolean supported = Arrays.stream(enumType.getEnumConstants())
                .anyMatch(item -> item.name().equals(value));
        Require.isTrue(supported,
                resource.getResourceType() + " field is invalid: " + fieldName + "=" + value);
        return Enum.valueOf(enumType, value);
    }

    private static Object fieldValue(ResourceDeclaration resource, String fieldName) {
        ResourceField field = resource.getFields().get(fieldName);
        return field == null ? null : field.getValue();
    }
}
