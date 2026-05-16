package io.mango.infra.kv.core.support;

import io.mango.common.result.Require;
import io.mango.infra.kv.api.IConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON converter implementation using Jackson ObjectMapper.
 * Uses JSON as intermediate format for object-to-object conversion.
 */
public class JsonConverter implements IConverter {

    private final ObjectMapper objectMapper;

    public JsonConverter() {
        this.objectMapper = new ObjectMapper();
    }

    public JsonConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T convert(Object source, Class<T> classType) {
        Require.notNull(source, "source cannot be null");
        Require.notNull(classType, "classType cannot be null");
        String json;
        try {
            json = objectMapper.writeValueAsString(source);
            return objectMapper.readValue(json, classType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to " + classType.getName(), e);
        }
    }
}
