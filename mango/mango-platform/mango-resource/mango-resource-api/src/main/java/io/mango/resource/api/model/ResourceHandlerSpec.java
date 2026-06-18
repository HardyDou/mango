package io.mango.resource.api.model;

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
}
