package io.mango.resource.api.model;

import io.mango.resource.api.enums.ResourceFieldType;
import lombok.Data;

/**
 * 资源字段声明。
 */
@Data
public class ResourceField {

    private ResourceFieldType type;
    private Object value;
    private String location;
    private String encoding;
    private String mediaType;
}
