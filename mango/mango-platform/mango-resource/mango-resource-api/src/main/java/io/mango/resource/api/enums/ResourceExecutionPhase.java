package io.mango.resource.api.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum ResourceExecutionPhase {
    BOOTSTRAP_REQUIRED,
    RUNTIME_EVENTUAL,
    MANUAL;

    @JsonCreator
    public static ResourceExecutionPhase from(String value) {
        if (value == null || value.isBlank()) {
            return BOOTSTRAP_REQUIRED;
        }
        return valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String value() {
        return name();
    }
}
