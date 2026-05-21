package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.Map;

@Schema(description = "实时消息业务载荷")
public final class RealtimePayload extends LinkedHashMap<String, Object> {

    public RealtimePayload() {
    }

    public RealtimePayload(Map<String, ?> source) {
        if (source != null) {
            putAll(source);
        }
    }

    public static RealtimePayload text(String text) {
        RealtimePayload payload = new RealtimePayload();
        payload.put("type", "text");
        payload.put("text", text == null ? "" : text);
        return payload;
    }

    public static RealtimePayload message(String message) {
        RealtimePayload payload = new RealtimePayload();
        payload.put("message", message == null ? "" : message);
        return payload;
    }

    public String textValue() {
        Object text = get("text");
        if (text != null) {
            return String.valueOf(text);
        }
        Object content = get("content");
        if (content != null) {
            return String.valueOf(content);
        }
        Object message = get("message");
        return message == null ? "" : String.valueOf(message);
    }
}
