package io.mango.notice.core.outbox;

import java.util.Collections;
import java.util.List;
import java.util.Map;

class MapPayload {

    private final Map<String, Object> payload;

    MapPayload(Map<String, Object> payload) {
        this.payload = payload == null ? Collections.emptyMap() : payload;
    }

    String stringValue(String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }

    Long longValue(String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    List<Long> longList(String key) {
        Object value = payload.get(key);
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(item -> item instanceof Number number ? number.longValue() : Long.valueOf(item.toString()))
                .toList();
    }

    <E extends Enum<E>> E enumValue(String key, Class<E> enumType, E defaultValue) {
        Object value = payload.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (enumType.isInstance(value)) {
            return enumType.cast(value);
        }
        return Enum.valueOf(enumType, value.toString());
    }
}
