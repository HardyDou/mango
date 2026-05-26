package io.mango.infra.sensitive.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Arrays;
import java.util.Map;

/**
 * Masks configured keys inside JSON strings.
 */
public final class SensitiveJsonMasker {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String[] DEFAULT_KEYS = {
            "password", "secret", "token", "appSecret", "accessKey", "secretKey", "privateKey", "credential"
    };

    private SensitiveJsonMasker() {
    }

    /**
     * Masks configured JSON key values recursively.
     *
     * @param json  raw JSON string
     * @param fuzzy whether key matching should be fuzzy
     * @param keys  configured keys
     * @return masked JSON string, or a key-style masked value when input is not valid JSON
     */
    public static String mask(String json, boolean fuzzy, String... keys) {
        if (json == null) {
            return null;
        }
        String[] effectiveKeys = effectiveKeys(keys);
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            maskNode(root, fuzzy, effectiveKeys);
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            return SensitiveMasker.key(json);
        }
    }

    private static String[] effectiveKeys(String[] keys) {
        if (keys == null || keys.length == 0 || Arrays.stream(keys).allMatch(key -> key == null || key.isBlank())) {
            return DEFAULT_KEYS;
        }
        return Arrays.stream(keys)
                .filter(key -> key != null && !key.isBlank())
                .toArray(String[]::new);
    }

    private static void maskNode(JsonNode node, boolean fuzzy, String[] keys) {
        if (node instanceof ObjectNode objectNode) {
            for (Map.Entry<String, JsonNode> field : objectNode.properties()) {
                if (matches(field.getKey(), fuzzy, keys)) {
                    String maskedValue = maskJsonValue(field.getValue());
                    objectNode.set(field.getKey(),
                            maskedValue == null ? NullNode.getInstance() : TextNode.valueOf(maskedValue));
                } else {
                    maskNode(field.getValue(), fuzzy, keys);
                }
            }
            return;
        }
        if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(child -> maskNode(child, fuzzy, keys));
        }
    }

    private static boolean matches(String actualKey, boolean fuzzy, String[] keys) {
        for (String key : keys) {
            if (SensitiveMasker.matchesKey(actualKey, fuzzy, key)) {
                return true;
            }
        }
        return false;
    }

    private static String maskJsonValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return SensitiveMasker.key(value.asText());
        }
        return SensitiveMasker.key(value.toString());
    }
}
