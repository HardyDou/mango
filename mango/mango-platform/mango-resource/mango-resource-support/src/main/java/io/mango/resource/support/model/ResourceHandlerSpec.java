package io.mango.resource.support.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;
import java.util.Set;

/**
 * 目标模块公开的资源字段契约。
 */
@Value
@Builder
public class ResourceHandlerSpec {

    String resourceType;
    @Singular
    Set<String> requiredFields;
    @Singular
    Map<String, String> fieldDescriptions;

    public ResourceHandlerSpec(String resourceType, Set<String> requiredFields,
                               Map<String, String> fieldDescriptions) {
        this.resourceType = resourceType;
        if (requiredFields == null) {
            this.requiredFields = Set.of();
        } else {
            this.requiredFields = Set.copyOf(requiredFields);
        }
        if (fieldDescriptions == null) {
            this.fieldDescriptions = Map.of();
        } else {
            this.fieldDescriptions = Map.copyOf(fieldDescriptions);
        }
    }

    public Set<String> getRequiredFields() {
        return Set.copyOf(requiredFields);
    }

    public Map<String, String> getFieldDescriptions() {
        return Map.copyOf(fieldDescriptions);
    }
}
