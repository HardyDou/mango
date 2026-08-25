package io.mango.ai.core.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.mango.ai.api.enums.AiCode;
import io.mango.common.result.Require;

import java.util.Iterator;
import java.util.Map;

/**
 * AI 服务运行时使用的受控 JSON Schema 校验器。
 *
 * <p>平台配置只接受对象 Schema；运行时覆盖业务服务需要的类型、必填字段、属性、数组项、枚举和边界约束。</p>
 */
final class AiJsonSchemaValidator {
    private static final int MAX_DEPTH = 32;

    private AiJsonSchemaValidator() {
    }

    static void validate(JsonNode schema, JsonNode value, AiCode code) {
        validateNode(schema, value, code, "$", 0);
    }

    private static void validateNode(JsonNode schema, JsonNode value, AiCode code, String path, int depth) {
        Require.isTrue(depth <= MAX_DEPTH, code, "JSON Schema 嵌套层级不能超过32层: " + path);
        Require.isTrue(schema != null && schema.isObject(), code, "JSON Schema 无效: " + path);
        Require.notNull(value, code, "JSON 值不能为空: " + path);
        JsonNode enumValues = schema.get("enum");
        if (enumValues != null) {
            boolean matched = false;
            for (JsonNode enumValue : enumValues) {
                if (enumValue.equals(value)) {
                    matched = true;
                    break;
                }
            }
            Require.isTrue(matched, code, "JSON 值不在 enum 范围内: " + path);
        }
        JsonNode type = schema.get("type");
        if (type != null && type.isTextual()) {
            Require.isTrue(matchesType(type.asText(), value), code, "JSON 类型不匹配: " + path);
        }
        if (value.isObject()) {
            validateObject(schema, value, code, path, depth);
        } else if (value.isArray()) {
            validateArray(schema, value, code, path, depth);
        } else if (value.isTextual()) {
            validateString(schema, value, code, path);
        } else if (value.isNumber()) {
            validateNumber(schema, value, code, path);
        }
    }

    private static void validateObject(JsonNode schema, JsonNode value, AiCode code, String path, int depth) {
        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            for (JsonNode field : required) {
                Require.isTrue(field.isTextual() && value.has(field.asText()), code,
                        "缺少必填字段: " + path + "." + field.asText());
            }
        }
        JsonNode properties = schema.get("properties");
        if (properties != null && properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (value.has(field.getKey())) {
                    validateNode(field.getValue(), value.get(field.getKey()), code,
                            path + "." + field.getKey(), depth + 1);
                }
            }
        }
        if (Boolean.FALSE.equals(schema.path("additionalProperties").asBoolean(true))) {
            Iterator<String> names = value.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                Require.isTrue(properties != null && properties.has(name), code,
                        "不允许的字段: " + path + "." + name);
            }
        }
    }

    private static void validateArray(JsonNode schema, JsonNode value, AiCode code, String path, int depth) {
        JsonNode minItems = schema.get("minItems");
        if (minItems != null && minItems.isIntegralNumber()) {
            Require.isTrue(value.size() >= minItems.asInt(), code, "数组元素数量不足: " + path);
        }
        JsonNode maxItems = schema.get("maxItems");
        if (maxItems != null && maxItems.isIntegralNumber()) {
            Require.isTrue(value.size() <= maxItems.asInt(), code, "数组元素数量超限: " + path);
        }
        JsonNode items = schema.get("items");
        if (items != null) {
            for (int index = 0; index < value.size(); index++) {
                validateNode(items, value.get(index), code, path + "[" + index + "]", depth + 1);
            }
        }
    }

    private static void validateString(JsonNode schema, JsonNode value, AiCode code, String path) {
        JsonNode minLength = schema.get("minLength");
        if (minLength != null && minLength.isIntegralNumber()) {
            Require.isTrue(value.textValue().length() >= minLength.asInt(), code, "字符串长度不足: " + path);
        }
        JsonNode maxLength = schema.get("maxLength");
        if (maxLength != null && maxLength.isIntegralNumber()) {
            Require.isTrue(value.textValue().length() <= maxLength.asInt(), code, "字符串长度超限: " + path);
        }
    }

    private static void validateNumber(JsonNode schema, JsonNode value, AiCode code, String path) {
        JsonNode minimum = schema.get("minimum");
        if (minimum != null && minimum.isNumber()) {
            Require.isTrue(value.decimalValue().compareTo(minimum.decimalValue()) >= 0, code, "数值小于 minimum: " + path);
        }
        JsonNode maximum = schema.get("maximum");
        if (maximum != null && maximum.isNumber()) {
            Require.isTrue(value.decimalValue().compareTo(maximum.decimalValue()) <= 0, code, "数值大于 maximum: " + path);
        }
    }

    private static boolean matchesType(String type, JsonNode value) {
        return switch (type) {
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "string" -> value.isTextual();
            case "number" -> value.isNumber();
            case "integer" -> value.isIntegralNumber();
            case "boolean" -> value.isBoolean();
            case "null" -> value.isNull();
            default -> false;
        };
    }
}
