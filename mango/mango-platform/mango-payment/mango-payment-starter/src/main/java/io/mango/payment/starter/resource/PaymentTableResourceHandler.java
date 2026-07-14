package io.mango.payment.starter.resource;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.payment.core.entity.PaymentBaseEntity;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.LongConsumer;

/**
 * Type-safe adapter from payment resource declarations to one payment configuration entity.
 *
 * @param <E> payment entity type
 */
final class PaymentTableResourceHandler<E extends PaymentBaseEntity> implements ResourceHandler {

    private static final Set<String> SECRET_CONFIG_KEYS = Set.of(
            "appsecret", "apisecret", "privatekey", "merchantkey", "gatewaymerchantkey", "mchntkey");

    private final BaseMapper<E> mapper;
    private final ObjectMapper objectMapper;
    private final Definition<E> definition;

    PaymentTableResourceHandler(BaseMapper<E> mapper, ObjectMapper objectMapper, Definition<E> definition) {
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
        LinkedHashMap<String, Object> entityValues = entityValues(resource, targetId);
        E existing = mapper.selectById(targetId);
        if (existing == null) {
            E entity = objectMapper.convertValue(entityValues, definition.entityType());
            mapper.insert(entity);
        } else {
            UpdateWrapper<E> update = new UpdateWrapper<>();
            update.eq("id", targetId);
            resource.getFields().forEach((fieldName, field) -> {
                if (!"targetId".equals(fieldName)) {
                    update.set(definition.fields().get(fieldName), databaseValue(fieldName, field));
                }
            });
            mapper.update(null, update);
        }
        return ResourceSyncResult.of(targetId, definition.table(), "Payment resource synced: " + resource.getBizKey());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Long targetId = targetId(resource);
        if (definition.statusColumn() == null) {
            if (definition.physicalDelete() == null) {
                throw new IllegalStateException("Payment resource cannot be disabled: " + resourceType());
            }
            definition.physicalDelete().accept(targetId);
        } else {
            UpdateWrapper<E> update = new UpdateWrapper<>();
            update.set(definition.statusColumn(), 0).eq("id", targetId);
            mapper.update(null, update);
        }
        return ResourceSyncResult.of(targetId, definition.table(), "Payment resource disabled: " + resource.getBizKey());
    }

    private LinkedHashMap<String, Object> entityValues(ResourceDeclaration resource, Long targetId) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("id", targetId);
        resource.getFields().forEach((fieldName, field) -> {
            if (!"targetId".equals(fieldName)) {
                values.put(fieldName, databaseValue(fieldName, field));
            }
        });
        return values;
    }

    private void validate(ResourceDeclaration resource) {
        if (resource == null || resource.getFields() == null) {
            throw new IllegalArgumentException("Payment resource fields are required: " + resourceType());
        }
        if (!resourceType().equals(resource.getResourceType())) {
            throw new IllegalArgumentException("Unexpected payment resource type: " + resource.getResourceType());
        }
        Set<String> unknownFields = new LinkedHashSet<>(resource.getFields().keySet());
        unknownFields.removeAll(definition.fields().keySet());
        if (!unknownFields.isEmpty()) {
            throw new IllegalArgumentException("Unsupported payment resource fields: " + unknownFields);
        }
        for (String requiredField : definition.requiredFields()) {
            ResourceField field = resource.getFields().get(requiredField);
            if (field == null || field.getValue() == null) {
                throw new IllegalArgumentException("Missing payment resource field: " + requiredField);
            }
        }
        validateSecretFreeConfiguration(resource);
    }

    private void validateSecretFreeConfiguration(ResourceDeclaration resource) {
        ResourceField configField = resource.getFields().get("configValuesJson");
        if (configField == null || configField.getValue() == null) {
            return;
        }
        try {
            JsonNode root;
            if (configField.getValue() instanceof String text) {
                root = objectMapper.readTree(text);
            } else {
                root = objectMapper.valueToTree(configField.getValue());
            }
            List<String> secretKeys = new ArrayList<>();
            collectSecretKeys(root, secretKeys);
            if (!secretKeys.isEmpty()) {
                throw new IllegalArgumentException("Payment resource must not contain merchant secrets: " + secretKeys);
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid payment configValuesJson", ex);
        }
    }

    private void collectSecretKeys(JsonNode node, List<String> secretKeys) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                if (SECRET_CONFIG_KEYS.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                    secretKeys.add(entry.getKey());
                }
                collectSecretKeys(entry.getValue(), secretKeys);
            });
        } else if (node.isArray()) {
            node.forEach(child -> collectSecretKeys(child, secretKeys));
        }
    }

    private Long targetId(ResourceDeclaration resource) {
        ResourceField field = resource.getFields().get("targetId");
        if (field == null || field.getValue() == null) {
            throw new IllegalArgumentException("Missing payment resource field: targetId");
        }
        Object value = field.getValue();
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return Long.valueOf(text.trim());
        }
        throw new IllegalArgumentException("Invalid payment targetId: " + value);
    }

    private Object databaseValue(String fieldName, ResourceField field) {
        if (field == null || field.getValue() == null) {
            return null;
        }
        if (field.getType() == null) {
            throw new IllegalArgumentException("Payment resource field type is required: " + fieldName);
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
            throw new IllegalArgumentException("Failed to serialize payment resource field: " + fieldName, ex);
        }
    }

    record Definition<E extends PaymentBaseEntity>(
            String resourceType,
            String table,
            Class<E> entityType,
            Map<String, String> fields,
            Set<String> requiredFields,
            List<String> dependencies,
            String statusColumn,
            LongConsumer physicalDelete) {

        Definition {
            fields = Map.copyOf(fields);
            requiredFields = Set.copyOf(requiredFields);
            dependencies = List.copyOf(dependencies);
        }
    }
}
