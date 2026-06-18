package io.mango.resource.api.builder;

import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builder for code based resource declarations.
 */
public final class ResourceDeclarationBuilder {

    private final ResourceDeclaration declaration = new ResourceDeclaration();
    private final Map<String, ResourceField> fields = new LinkedHashMap<>();

    private ResourceDeclarationBuilder(String resourceType) {
        declaration.setResourceType(resourceType);
    }

    public static ResourceDeclarationBuilder create(String resourceType) {
        return new ResourceDeclarationBuilder(resourceType);
    }

    public ResourceDeclarationBuilder id(String id) {
        declaration.setId(id);
        return this;
    }

    public ResourceDeclarationBuilder version(Integer version) {
        declaration.setVersion(version);
        return this;
    }

    public ResourceDeclarationBuilder resourceType(String resourceType) {
        declaration.setResourceType(resourceType);
        return this;
    }

    public ResourceDeclarationBuilder module(String moduleCode, String moduleName) {
        declaration.setModuleCode(moduleCode);
        declaration.setModuleName(moduleName);
        return this;
    }

    public ResourceDeclarationBuilder moduleCode(String moduleCode) {
        declaration.setModuleCode(moduleCode);
        return this;
    }

    public ResourceDeclarationBuilder moduleName(String moduleName) {
        declaration.setModuleName(moduleName);
        return this;
    }

    public ResourceDeclarationBuilder bizKey(String bizKey) {
        declaration.setBizKey(bizKey);
        return this;
    }

    public ResourceDeclarationBuilder name(String name) {
        declaration.setName(name);
        return this;
    }

    public ResourceDeclarationBuilder targetModule(String targetModule) {
        declaration.setTargetModule(targetModule);
        return this;
    }

    public ResourceDeclarationBuilder syncMode(ResourceSyncMode syncMode) {
        if (syncMode != null) {
            declaration.setSyncMode(syncMode);
        }
        return this;
    }

    public ResourceDeclarationBuilder status(ResourceStatus status) {
        if (status != null) {
            declaration.setStatus(status);
        }
        return this;
    }

    public ResourceDeclarationBuilder field(String name, ResourceField field) {
        if (hasText(name) && field != null) {
            fields.put(name, field);
        }
        return this;
    }

    public ResourceDeclarationBuilder field(String name, ResourceFieldType type, Object value) {
        if (value == null || isBlankString(value)) {
            return this;
        }
        return field(name, ResourceFields.of(type, value));
    }

    public ResourceDeclarationBuilder string(String name, String value) {
        return field(name, ResourceFieldType.STRING, value);
    }

    public ResourceDeclarationBuilder intValue(String name, Integer value) {
        return field(name, ResourceFieldType.INT, value);
    }

    public ResourceDeclarationBuilder longValue(String name, Long value) {
        return field(name, ResourceFieldType.LONG, value);
    }

    public ResourceDeclarationBuilder decimal(String name, BigDecimal value) {
        return field(name, ResourceFieldType.DECIMAL, value);
    }

    public ResourceDeclarationBuilder bool(String name, Boolean value) {
        return field(name, ResourceFieldType.BOOLEAN, value);
    }

    public ResourceDeclarationBuilder date(String name, LocalDate value) {
        return field(name, ResourceFieldType.DATE, value);
    }

    public ResourceDeclarationBuilder dateTime(String name, LocalDateTime value) {
        return field(name, ResourceFieldType.DATETIME, value);
    }

    public ResourceDeclarationBuilder json(String name, Object value) {
        return field(name, ResourceFieldType.JSON, value);
    }

    public ResourceDeclarationBuilder object(String name, Object value) {
        return field(name, ResourceFieldType.OBJECT, value);
    }

    public ResourceDeclarationBuilder list(String name, Object value) {
        return field(name, ResourceFieldType.LIST, value);
    }

    public ResourceDeclarationBuilder file(String name, String location) {
        if (!hasText(location)) {
            return this;
        }
        return field(name, ResourceFields.file(location));
    }

    public ResourceDeclarationBuilder file(String name, String location, String encoding, String mediaType) {
        if (!hasText(location)) {
            return this;
        }
        return field(name, ResourceFields.file(location, encoding, mediaType));
    }

    public ResourceDeclaration build() {
        declaration.setFields(new LinkedHashMap<>(fields));
        return declaration;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isBlankString(Object value) {
        return value instanceof String stringValue && stringValue.isBlank();
    }
}
