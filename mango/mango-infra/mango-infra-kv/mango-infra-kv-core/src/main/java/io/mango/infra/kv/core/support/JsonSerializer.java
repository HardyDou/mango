package io.mango.infra.kv.core.support;

import io.mango.common.result.Require;
import io.mango.infra.kv.api.ISerializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON serializer implementation using Jackson ObjectMapper.
 */
public class JsonSerializer implements ISerializer {

    private final ObjectMapper objectMapper;

    public JsonSerializer() {
        this.objectMapper = new ObjectMapper();
    }

    public JsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(Object object) {
        Require.notNull(object, "object cannot be null");
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    @Override
    public <T> T deserialize(String content, Class<T> classType) {
        Require.notBlank(content, "content cannot be null or blank");
        Require.notNull(classType, "classType cannot be null");
        try {
            return objectMapper.readValue(content, classType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize content", e);
        }
    }
}
