package io.mango.resource.api.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * 资源同步模式。
 */
public enum ResourceSyncMode {
    AUTO,
    INIT_ONLY,
    MANUAL,
    LOCKED;

    @JsonCreator
    public static ResourceSyncMode from(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        String normalized = value.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        return ResourceSyncMode.valueOf(normalized);
    }

    @JsonValue
    public String value() {
        return name();
    }
}
