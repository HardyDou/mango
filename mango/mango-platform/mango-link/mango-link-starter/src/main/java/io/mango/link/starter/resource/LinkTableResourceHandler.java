package io.mango.link.starter.resource;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.persistence.api.entity.TenantEntity;
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

/** 将 Link 资源声明同步到模块自有业务表。 */
final class LinkTableResourceHandler<E extends TenantEntity> implements ResourceHandler {

    private static final String TARGET_ID = "targetId";

    private final BaseMapper<E> mapper;
    private final ObjectMapper objectMapper;
    private final Definition<E> definition;

    LinkTableResourceHandler(BaseMapper<E> mapper, ObjectMapper objectMapper, Definition<E> definition) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.definition = definition;
    }

    @Override
    public String resourceType() {
        return definition.resourceType;
    }

    @Override
    public List<String> dependsOnResourceTypes() {
        return definition.dependencies;
    }

    @Override
    public ResourceHandlerSpec spec() {
        ResourceHandlerSpec.ResourceHandlerSpecBuilder builder = ResourceHandlerSpec.builder()
                .resourceType(resourceType());
        definition.requiredFields.forEach(builder::requiredField);
        definition.fields.forEach((field, column) -> builder.fieldDescription(
                field, "写入 " + definition.table + "." + column + "。"));
        return builder.build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        validate(resource);
        Long targetId = targetId(resource);
        E existing = mapper.selectById(targetId);
        if (existing == null) {
            mapper.insert(objectMapper.convertValue(entityValues(resource, targetId), definition.entityType));
        } else {
            UpdateWrapper<E> update = new UpdateWrapper<>();
            update.eq("id", targetId);
            resource.getFields().forEach((fieldName, field) -> {
                if (!TARGET_ID.equals(fieldName)) {
                    update.set(definition.fields.get(fieldName), value(field));
                }
            });
            mapper.update(null, update);
        }
        return result(targetId, "资源已同步");
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Long targetId = targetId(resource);
        if (definition.disableMode == DisableMode.DELETE) {
            mapper.deleteById(targetId);
        } else {
            UpdateWrapper<E> update = new UpdateWrapper<>();
            update.eq("id", targetId).set("status", "DISABLED");
            mapper.update(null, update);
        }
        return result(targetId, "资源已停用");
    }

    private ResourceSyncResult result(Long targetId, String action) {
        return ResourceSyncResult.of(targetId, definition.table,
                "Link " + action + ": " + resourceType());
    }

    private Map<String, Object> entityValues(ResourceDeclaration resource, Long targetId) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", targetId);
        resource.getFields().forEach((fieldName, field) -> {
            if (!TARGET_ID.equals(fieldName)) {
                values.put(fieldName, value(field));
            }
        });
        return values;
    }

    private void validate(ResourceDeclaration resource) {
        if (resource == null || resource.getFields() == null) {
            throw new IllegalArgumentException("Link 资源字段不能为空: " + resourceType());
        }
        if (!resourceType().equals(resource.getResourceType())) {
            throw new IllegalArgumentException("Link 资源类型不匹配: " + resource.getResourceType());
        }
        Set<String> unknownFields = new LinkedHashSet<>(resource.getFields().keySet());
        unknownFields.removeAll(definition.fields.keySet());
        if (!unknownFields.isEmpty()) {
            throw new IllegalArgumentException("Link 资源包含不支持字段: " + unknownFields);
        }
        for (String requiredField : definition.requiredFields) {
            ResourceField field = resource.getFields().get(requiredField);
            if (field == null || field.getValue() == null) {
                throw new IllegalArgumentException("Link 资源缺少字段: " + requiredField);
            }
        }
    }

    private Long targetId(ResourceDeclaration resource) {
        ResourceField field = resource.getFields().get(TARGET_ID);
        if (field == null || field.getValue() == null) {
            throw new IllegalArgumentException("Link 资源缺少字段: targetId");
        }
        Object value = field.getValue();
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return Long.valueOf(text.trim());
        }
        throw new IllegalArgumentException("Link 资源 targetId 非法: " + value);
    }

    private Object value(ResourceField field) {
        return field == null ? null : field.getValue();
    }

    enum DisableMode {
        STATUS,
        DELETE
    }

    static final class Definition<E extends TenantEntity> {

        private final String resourceType;
        private final String table;
        private final Class<E> entityType;
        private final Map<String, String> fields;
        private final Set<String> requiredFields;
        private final List<String> dependencies;
        private final DisableMode disableMode;

        Definition(String resourceType,
                   String table,
                   Class<E> entityType,
                   Map<String, String> fields,
                   Set<String> requiredFields,
                   List<String> dependencies,
                   DisableMode disableMode) {
            this.resourceType = resourceType;
            this.table = table;
            this.entityType = entityType;
            this.fields = Map.copyOf(fields);
            this.requiredFields = Set.copyOf(requiredFields);
            this.dependencies = List.copyOf(dependencies);
            this.disableMode = disableMode;
        }
    }
}
