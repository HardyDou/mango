package io.mango.notice.api.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/** Arbitrary JSON object returned by notice APIs through a typed protocol boundary. */
@Data
@NoArgsConstructor
public class NoticeJsonVO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final JavaType MAP_TYPE = OBJECT_MAPPER.getTypeFactory()
            .constructMapType(LinkedHashMap.class, String.class, Object.class);

    @Schema(description = "JSON 对象内容")
    private String json = "{}";

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public NoticeJsonVO(JsonNode value) {
        this.json = value == null || value.isNull() ? "{}" : value.toString();
    }

    public static NoticeJsonVO of(Map<String, ?> value) {
        return new NoticeJsonVO(OBJECT_MAPPER.valueToTree(value == null ? Map.of() : value));
    }

    public Map<String, Object> toMap() {
        try {
            return OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("通知 JSON 对象格式错误", ex);
        }
    }

    @JsonValue
    public JsonNode value() {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("通知 JSON 对象格式错误", ex);
        }
    }
}
