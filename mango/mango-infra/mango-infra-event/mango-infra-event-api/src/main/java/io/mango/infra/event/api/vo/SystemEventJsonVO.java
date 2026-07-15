package io.mango.infra.event.api.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Arbitrary event JSON represented by a typed protocol model. */
@Data
@NoArgsConstructor
public class SystemEventJsonVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final JavaType MAP_TYPE = OBJECT_MAPPER.getTypeFactory()
            .constructMapType(LinkedHashMap.class, String.class, Object.class);

    @Schema(description = "JSON 对象内容")
    private String json = "{}";

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public SystemEventJsonVO(JsonNode value) {
        if (value == null || value.isNull()) {
            this.json = "{}";
        } else {
            this.json = value.toString();
        }
    }

    public static SystemEventJsonVO of(Map<String, ?> value) {
        Map<String, ?> source = value;
        if (source == null) {
            source = Map.of();
        }
        return new SystemEventJsonVO(OBJECT_MAPPER.valueToTree(source));
    }

    public Map<String, Object> toMap() {
        try {
            return OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("系统事件 JSON 对象格式错误", ex);
        }
    }

    public <T> Map<String, T> toMap(Class<T> valueType) {
        Map<String, Object> values = toMap();
        Map<String, T> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(key, OBJECT_MAPPER.convertValue(value, valueType)));
        return result;
    }

    @JsonValue
    public JsonNode value() {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("系统事件 JSON 对象格式错误", ex);
        }
    }
}
