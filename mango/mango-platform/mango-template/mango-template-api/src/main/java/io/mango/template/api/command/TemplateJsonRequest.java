package io.mango.template.api.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模板接口接收任意 JSON 对象的类型化边界。
 */
public final class TemplateJsonRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final JavaType MAP_TYPE = OBJECT_MAPPER.getTypeFactory()
            .constructMapType(LinkedHashMap.class, String.class, Object.class);

    @NotBlank(message = "模板变量 JSON 不能为空")
    @Size(max = 10485760, message = "模板变量 JSON 不能超过10MB")
    @Schema(description = "JSON 对象内容")
    private final String json;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public TemplateJsonRequest(JsonNode value) {
        JsonNode resolved = value;
        if (resolved == null || resolved.isNull()) {
            resolved = OBJECT_MAPPER.createObjectNode();
        }
        if (!resolved.isObject()) {
            throw new IllegalArgumentException("模板变量必须是 JSON 对象");
        }
        this.json = resolved.toString();
    }

    private TemplateJsonRequest(String json) {
        this.json = json;
    }

    public static TemplateJsonRequest of(Map<String, ?> value) {
        Map<String, ?> resolved = value;
        if (resolved == null) {
            resolved = Map.of();
        }
        return new TemplateJsonRequest(OBJECT_MAPPER.valueToTree(resolved));
    }

    public TemplateJsonRequest copy() {
        return new TemplateJsonRequest(json);
    }

    public Map<String, Object> toMap() {
        try {
            return OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("模板变量 JSON 格式错误", exception);
        }
    }

    @JsonValue
    public JsonNode value() {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("模板变量 JSON 格式错误", exception);
        }
    }
}
