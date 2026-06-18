package io.mango.resource.api.builder;

import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceField;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resource field factory methods for code based providers.
 */
public final class ResourceFields {

    private ResourceFields() {
    }

    public static ResourceField of(ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        return field;
    }

    public static ResourceField string(String value) {
        return of(ResourceFieldType.STRING, value);
    }

    public static ResourceField intValue(Integer value) {
        return of(ResourceFieldType.INT, value);
    }

    public static ResourceField longValue(Long value) {
        return of(ResourceFieldType.LONG, value);
    }

    public static ResourceField decimal(BigDecimal value) {
        return of(ResourceFieldType.DECIMAL, value);
    }

    public static ResourceField bool(Boolean value) {
        return of(ResourceFieldType.BOOLEAN, value);
    }

    public static ResourceField date(LocalDate value) {
        return of(ResourceFieldType.DATE, value);
    }

    public static ResourceField dateTime(LocalDateTime value) {
        return of(ResourceFieldType.DATETIME, value);
    }

    public static ResourceField json(Object value) {
        return of(ResourceFieldType.JSON, value);
    }

    public static ResourceField object(Object value) {
        return of(ResourceFieldType.OBJECT, value);
    }

    public static ResourceField list(Object value) {
        return of(ResourceFieldType.LIST, value);
    }

    public static ResourceField file(String location) {
        ResourceField field = of(ResourceFieldType.FILE, null);
        field.setLocation(location);
        return field;
    }

    public static ResourceField file(String location, String encoding, String mediaType) {
        ResourceField field = file(location);
        field.setEncoding(encoding);
        field.setMediaType(mediaType);
        return field;
    }
}
