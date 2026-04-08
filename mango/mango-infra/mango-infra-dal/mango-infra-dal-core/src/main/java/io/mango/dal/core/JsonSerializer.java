package io.mango.dal.core;

import io.mango.dal.api.ISerializer;

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
        if (object == null) {
            throw new IllegalArgumentException("object cannot be null");
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    @Override
    public <T> T deserialize(String content, Class<T> classType) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be null or blank");
        }
        if (classType == null) {
            throw new IllegalArgumentException("classType cannot be null");
        }
        try {
            return objectMapper.readValue(content, classType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize content", e);
        }
    }
}