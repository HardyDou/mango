package io.mango.cms.starter.resource;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.cms.core.entity.CmsBaseTenantEntity;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Type-safe adapter from CMS resource declarations to a CMS-owned table.
 *
 * @param <E> CMS entity type
 */
final class CmsTableResourceHandler<E extends CmsBaseTenantEntity> implements ResourceHandler {

    private static final String TARGET_ID = "targetId";

    private final BaseMapper<E> mapper;
    private final ObjectMapper objectMapper;
    private final Definition<E> definition;

    CmsTableResourceHandler(BaseMapper<E> mapper, ObjectMapper objectMapper, Definition<E> definition) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.definition = definition;
    }

    @Override
    public String resourceType() {
        return definition.resourceType();
    }

    @Override
    public List<String> dependsOnResourceTypes() {
        return definition.dependencies();
    }

    @Override
    public ResourceHandlerSpec spec() {
        ResourceHandlerSpec.ResourceHandlerSpecBuilder builder = ResourceHandlerSpec.builder()
                .resourceType(resourceType());
        definition.requiredFields().forEach(builder::requiredField);
        definition.fields().forEach((field, column) -> builder.fieldDescription(
                field, "写入 " + definition.table() + "." + column + "。"));
        return builder.build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        validate(resource);
        Long targetId = targetId(resource);
        E existing = mapper.selectById(targetId);
        if (existing == null) {
            E entity = objectMapper.convertValue(entityValues(resource, targetId), definition.entityType());
            entity.setDeleted(0);
            mapper.insert(entity);
        } else {
            UpdateWrapper<E> update = new UpdateWrapper<>();
            update.eq("id", targetId).set("deleted", 0);
            resource.getFields().forEach((fieldName, field) -> {
                if (!TARGET_ID.equals(fieldName)) {
                    update.set(definition.fields().get(fieldName), databaseValue(fieldName, field));
                }
            });
            mapper.update(null, update);
        }
        return ResourceSyncResult.of(targetId, definition.table(), "CMS resource synced: " + resource.getBizKey());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Long targetId = targetId(resource);
        UpdateWrapper<E> update = new UpdateWrapper<>();
        update.set("deleted", 1).eq("id", targetId);
        mapper.update(null, update);
        return ResourceSyncResult.of(targetId, definition.table(), "CMS resource disabled: " + resource.getBizKey());
    }

    private LinkedHashMap<String, Object> entityValues(ResourceDeclaration resource, Long targetId) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("id", targetId);
        resource.getFields().forEach((fieldName, field) -> {
            if (!TARGET_ID.equals(fieldName)) {
                values.put(fieldName, databaseValue(fieldName, field));
            }
        });
        return values;
    }

    private void validate(ResourceDeclaration resource) {
        if (resource == null || resource.getFields() == null) {
            throw new IllegalArgumentException("CMS resource fields are required: " + resourceType());
        }
        if (!resourceType().equals(resource.getResourceType())) {
            throw new IllegalArgumentException("Unexpected CMS resource type: " + resource.getResourceType());
        }
        Set<String> unknownFields = new LinkedHashSet<>(resource.getFields().keySet());
        unknownFields.removeAll(definition.fields().keySet());
        if (!unknownFields.isEmpty()) {
            throw new IllegalArgumentException("Unsupported CMS resource fields: " + unknownFields);
        }
        for (String requiredField : definition.requiredFields()) {
            ResourceField field = resource.getFields().get(requiredField);
            if (field == null || field.getValue() == null) {
                throw new IllegalArgumentException("Missing CMS resource field: " + requiredField);
            }
        }
    }

    private Long targetId(ResourceDeclaration resource) {
        ResourceField field = resource.getFields().get(TARGET_ID);
        if (field == null || field.getValue() == null) {
            throw new IllegalArgumentException("Missing CMS resource field: targetId");
        }
        Object value = field.getValue();
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return Long.valueOf(text.trim());
        }
        throw new IllegalArgumentException("Invalid CMS targetId: " + value);
    }

    private Object databaseValue(String fieldName, ResourceField field) {
        if (field == null || field.getValue() == null) {
            return null;
        }
        if (field.getType() == null) {
            throw new IllegalArgumentException("CMS resource field type is required: " + fieldName);
        }
        return switch (field.getType()) {
            case JSON, OBJECT, LIST -> writeJson(fieldName, field.getValue());
            default -> field.getValue();
        };
    }

    private String writeJson(String fieldName, Object value) {
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize CMS resource field: " + fieldName, ex);
        }
    }

    record Definition<E extends CmsBaseTenantEntity>(
            String resourceType,
            String table,
            Class<E> entityType,
            Map<String, String> fields,
            Set<String> requiredFields,
            List<String> dependencies) {

        Definition {
            fields = Map.copyOf(fields);
            requiredFields = Set.copyOf(requiredFields);
            dependencies = List.copyOf(dependencies);
        }
    }
}
