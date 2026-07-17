package io.mango.template.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 模板领域 JSON 编解码器。
 */
public final class TemplateJsonCodec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private TemplateJsonCodec() {
    }

    public static <T> T read(String content, TypeReference<T> type) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(content, type);
    }

    public static JsonNode readTree(String content) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(content);
    }

    public static String write(Object value) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(value);
    }
}
