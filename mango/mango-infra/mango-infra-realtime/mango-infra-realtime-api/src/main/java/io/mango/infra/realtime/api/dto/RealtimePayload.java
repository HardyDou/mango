package io.mango.infra.realtime.api.dto;

import io.mango.common.contract.LocalCapabilityContract;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.Map;

@LocalCapabilityContract
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
        payload.put("text", emptyIfNull(text));
        return payload;
    }

    public static RealtimePayload message(String message) {
        RealtimePayload payload = new RealtimePayload();
        payload.put("message", emptyIfNull(message));
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
        if (message == null) {
            return "";
        }
        return String.valueOf(message);
    }

    private static String emptyIfNull(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
